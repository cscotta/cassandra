# Implementation Plan: an `io_uring` `disk_access_mode` for Apache Cassandra (JDK 25 + Panama FFM)

**Status:** Design proposal / feasibility-grade implementation plan
**Scope:** Async SSTable **read** path backed by Linux `io_uring`, via a Netty-free JDK 25 Foreign Function & Memory (FFM) binding.
**Target branch context:** `cscotta/io_uring`; depends on the JDK 25 support work (`JDK25_SUPPORT_PLAN.md`).
**Author aid:** synthesized from a fan-out codebase exploration (six parallel investigations). File:line references are current as of this branch.

---

## 0. Executive summary

Cassandra's SSTable read path is **synchronous and thread-per-request**: a `ReadStage` thread (default `concurrent_reads = 32`, `Config.java:251`) calls `Rebufferer.rebuffer(pos)` (`Rebufferer.java:36`), which blocks on `ChannelProxy.read()` → `FileChannel.read(buf, pos)` (`ChannelProxy.java:169-180`). `io_uring`'s value is *asynchronous, batched* I/O; to realize it we must stop blocking a thread per read and drive reads from completion events.

The historically hard part of "Option B — true async read path" is that `RandomAccessReader` is a `DataInputPlus` byte stream: **every** `readInt()`/`readFully()` may trigger a `reBuffer()`, so making rebuffering return a future forces a continuation-passing rewrite of the entire deserialization stack (~30–40 files; see §2). That is exactly why prior async/TPC read-path efforts never merged to Apache trunk.

**The key enabler on JDK 25 is stable virtual threads (Loom).** A `ReadStage` task running on a virtual thread can *look* fully synchronous and still yield the underlying carrier thread when it blocks on an `io_uring` completion future — no CPS rewrite required. This collapses most of the blast radius of Option B into "issue the read via the ring, block the virtual thread on the completion, let Loom unmount the carrier." We therefore recommend a **phased plan** that:

1. Builds and proves the FFM `io_uring` binding in isolation (microbenchmarks) **before** touching the read path.
2. Introduces async at exactly one seam — `ChunkCache` / `ChunkReader` — and uses **virtual-thread mounting** (not a CPS rewrite) to absorb the asynchrony. This is a bounded, revertible change.
3. Only if microbench + macro data justify it, pursues the full CPS async iterator path (§2, Strategy A) for the hot cache-miss path.

Each phase has an explicit go/no-go gate (§10). If Phase 0 microbenchmarks don't show io_uring beating `direct` (O_DIRECT) at realistic queue depths, **stop** — the refactor isn't worth it.

> **Reality check that bounds the entire effort:** io_uring only helps **cache misses that reach the device**. Hot reads are served from the `ChunkCache` (off-heap, `ChunkCache.java`) or the OS page cache and issue no syscall. The win is concentrated in: (a) high-concurrency, cache-cold point reads, (b) large scans / compaction reads, (c) tail latency under device saturation on NVMe. The common hot-cache path must be a **no-op / no-regression**.

---

## 1. Current read-path architecture (baseline)

### 1.1 The synchronous call chain (bottom → top)

| # | Layer | Location | Notes |
|---|---|---|---|
| 1 | `FileChannel.read(buf, pos)` | `ChannelProxy.java:174` | Blocking positional pread. `direct` mode opens with `ExtendedOpenOption.DIRECT` (`ChannelProxy.java:69-80`). |
| 2 | `ChunkReader.readChunk(pos, buf)` | `SimpleChunkReader.java:37-43`; `CompressedChunkReader.java:297` | Fills one chunk buffer. |
| 3 | `Rebufferer.rebuffer(pos)` → `BufferHolder` | `Rebufferer.java:36`; `BufferManagingRebufferer.java:77-82`; `ChunkCache.CachingRebufferer.rebuffer` `ChunkCache.java:234-251` | **The sync seam.** Cache path calls Caffeine `LoadingCache.get()` → `ChunkCache.load()` (`:160-174`), which blocks. |
| 4 | `RandomAccessReader.reBufferAt(pos)` | `RandomAccessReader.java:79-88` | `@NotThreadSafe`; one reader per operation. Every `DataInputPlus` read may re-buffer. |
| 5 | BTI trie walk `Walker.go(pos)` | `Walker.java:94-109` | `PartitionIndex.Reader` holds a `Rebufferer` directly. |
| 6 | Index lookup | Big: `BigTableReader.getRowIndexEntry` `:251-403`; BTI: `BtiTableReader.getExactPosition` `:235-294` | Bloom filter + index summary are **in-memory** (no I/O); key-cache hit skips index reads entirely. |
| 7 | `UnfilteredRowIterator` / `AbstractSSTableIterator` | `AbstractSSTableIterator.java:76` | `next()` pulls rows; each pull may hit disk. |
| 8 | `SinglePartitionReadCommand.queryStorage` | `SinglePartitionReadCommand.java:556` | Merges memtables + SSTables; lazy `UnfilteredRowIterators.merge`. |
| 9 | `ReadCommand.executeLocally` | `ReadCommand.java:506-534` | `createResponse(...)` materializes the lazy chain → **all disk reads fire here**. |
| 10 | `StorageProxy.LocalReadRunnable.runMayThrow` | `StorageProxy.java:2740-2786` | Runs on `Stage.READ`; signals `ReadCallback.condition`. |
| 11 | `AbstractReadExecutor.awaitResponses` | `AbstractReadExecutor.java:424` | Native-transport thread blocks on `Condition.Async`. |
| 12–13 | `StorageProxy.fetchRows` → `SelectStatement.execute` → `Dispatcher` | `StorageProxy.java:2649`; `Dispatcher.java:317` | Response flushed to client on the native transport thread. |

### 1.2 Disk-read budget for a point read

- Bloom filter: in-memory (off-heap `IFilter`), no I/O.
- Key cache hit: eliminates **all** index reads → only the data read remains.
- Index summary (Big) / partition index trie (BTI): summary is in-memory; trie walk is O(log N) page touches through the `ChunkCache`.
- Data: 1..N 4 KiB-ish chunk reads through the `ChunkCache`.

**Implication:** a warm read touches disk rarely; the async machinery must impose ~zero overhead when everything is cached.

### 1.3 No existing async scaffolding

Greps for `TPC`, `Flow`, `StagedScheduler`, `rebufferAsync`, `AsyncCacheLoader` in `io/util`, `db`, `io/sstable` return **nothing** in the read path. The only reusable async primitives are `AsyncPromise`/`AsyncFuture` (`utils/concurrent/`, Netty-derived, used by Accord/messaging) and `Condition.Async` (used by `ReadCallback`). There is **no** FFM/`MemorySegment`/`Linker` usage anywhere in `src/` today.

---

## 2. The core problem: turning a synchronous byte stream async

`RandomAccessReader implements FileDataInput extends DataInputPlus`. There is no API to pre-declare which byte ranges a deserializer will need; asynchrony **cannot be hidden below** `RandomAccessReader`. Three strategies exist, in increasing blast radius / performance ceiling.

### Strategy A — Full continuation-passing (async iterators) *(highest ceiling, highest cost)*

Thread `CompletableFuture<BufferHolder> rebufferAsync(pos)` up through `RandomAccessReader` → SSTable iterators → `UnfilteredRowIterators.merge` → `queryStorage` → `executeLocally`. Every method that touches `FileDataInput` becomes a future chain.

- **Interfaces to change:** `Rebufferer`, `ChunkReader` (2 core seams); 7 `Rebufferer` impls + `CachingRebufferer`; `RandomAccessReader`, `Walker`, `PartitionIndex.Reader`, `BigTableReader.getRowIndexEntry`, `BtiTableReader.getExactPosition`, `AbstractSSTableIterator`, `SSTableReader.rowIterator/partitionIterator`, `SinglePartitionReadCommand.queryStorage`, `ReadCommand.executeLocally`, `LocalReadRunnable`.
- **Blast radius:** ~30–40 production files; `DataInputPlus`/`FileDataInput` are the widest-impact interfaces.
- **Verdict:** This is the version that historically never merged. Reserve for a targeted, measured hot-path optimization *after* the win is proven — never as the first step.

### Strategy B — Prefetch + bounded parallel submit, synchronous consume *(medium)*

Keep the synchronous read logic, but exploit the **known-parallel** reads a single query issues: fan out `io_uring` submissions across the multiple SSTables a partition read touches, and across the chunks of a scan (readahead), then consume synchronously. Captures batched submission and device queue depth without CPS. Requires a prefetch coordinator but leaves `DataInputPlus` untouched. Useful mainly for scans/compaction where the access pattern is predictable.

### Strategy C — Virtual-thread mounting (Loom) *(lowest blast radius; the recommended primary vehicle)* ★

On **JDK 25 virtual threads are a stable feature.** Run each `ReadStage`/local-read task on a virtual thread. The read code stays *textually synchronous*: at the one io_uring seam we do `future.get()` on the completion. Because a virtual thread blocked on a `CompletableFuture` **unmounts its carrier**, the OS thread is freed to run other reads. You get M:N I/O concurrency and device queue depth **without** rewriting the deserialization stack.

- **What it buys:** the async concurrency benefit (many in-flight device reads, carriers not blocked) at the cost of *one* seam change (§3/§5), not 40.
- **What it does *not* buy by itself:** fewer syscalls. Virtual threads still need the reads *submitted in batches to a shared ring* to cut syscall count — that is what the FFM binding + shared ring (§3) provide. Loom handles the *waiting*; the ring handles the *batching*.
- **Caveats to design around:**
  - **Pinning.** A virtual thread pinned by a `synchronized` block or a native (FFM downcall) frame *while blocked* will not unmount. JDK 24+ (JEP 491) removed `synchronized` pinning, so on JDK 25 the main remaining risk is blocking *inside* a native downcall — which we avoid: the FFM downcall (`io_uring_enter`) returns immediately; the *wait* is a `future.get()` on plain Java state, which unmounts cleanly. Audit the read path for `synchronized` that wraps a blocking wait regardless.
  - **Caffeine `ChunkCache` currently uses `ImmediateExecutor` and a synchronous `LoadingCache`** (`ChunkCache.java:152,160`); under virtual threads the load runs on the virtual thread and blocks it — fine, because the carrier unmounts. But for **in-flight dedup** we still want `AsyncLoadingCache` (§4).
  - **ThreadLocals / `FastThreadLocal`.** `BufferPool` uses `FastThreadLocal<LocalPool>` (§4.3). Millions of short-lived virtual threads would thrash thread-local pools. Mitigation: keep a **bounded pool of carrier/platform threads** for the actual submit/consume and use virtual threads only as the suspension vehicle, or pin buffer allocation to the completion/carrier layer. This is a real design task, addressed in §3.3.
  - Not simulator-compatible (§8.6) — simulator stays on `standard`.

**Recommendation:** Adopt **Strategy C as the primary mechanism**, keep **Strategy B (readahead) as a scan/compaction optimization**, and hold **Strategy A in reserve** for a proven hot-path bottleneck. The phased plan (§7) reflects this.

---

## 3. Component 1 — FFM `io_uring` binding (Netty-free)

New package: `org.apache.cassandra.io.uring`. Netty's io_uring transport is socket-oriented and exposes no file API, so we bind the kernel interface directly with JDK 25 FFM. This also matches the direction of travel away from JNI/JNA (which emits JEP 472 warnings on JDK 24+).

### 3.1 Syscalls

io_uring has **no glibc wrappers**. Three raw syscalls:

- `io_uring_setup(u32 entries, io_uring_params*) → ring_fd`
- `io_uring_enter(u32 fd, u32 to_submit, u32 min_complete, u32 flags, sigset_t*, size_t) → n`
- `io_uring_register(u32 fd, u32 opcode, void* arg, u32 nr_args) → n`

x86-64 and aarch64 syscall numbers are **425 / 426 / 427**. Two binding options:

- **Option A (preferred): bind libc `syscall`** via `Linker.nativeLinker()` + `SymbolLookup`, one fixed-arity downcall handle per io_uring syscall (prepend the per-arch syscall number as the first `long`). Use `Linker.Option.captureCallState("errno")` (JDK 22+) to read `errno`, and `Linker.Option.firstVariadicArg(...)` for the variadic `syscall`.
- **Option B (fallback): a ~30-line C shim** `libcassandra_uring.so` exporting three named wrappers, bound by name. Easier to audit; adds a native artifact to ship per-arch. Design the binding to try A, fall back to B.

Also bind libc `mmap`/`munmap` (for ring memory) the same way.

### 3.2 Memory layouts (`MemoryLayout` / `VarHandle`)

Model exactly to kernel ABI (`<linux/io_uring.h>`): `io_uring_params` (120 B, incl. `io_sqring_offsets` @40, `io_cqring_offsets` @80), `io_uring_sqe` (64 B), `io_uring_cqe` (16 B: `user_data`, `res`, `flags`), `struct iovec`. Ring memory via `mmap` at offsets `IORING_OFF_SQ_RING`, `IORING_OFF_CQ_RING`, `IORING_OFF_SQES` (2 mmaps with `IORING_FEAT_SINGLE_MMAP`, kernel ≥ 5.4). Wrap returned addresses with `MemorySegment.ofAddress(a).reinterpret(size, arena, …)`. Ring head/tail/flags are shared with the kernel → use `VarHandle.getAcquire()/setRelease()` for the correct memory ordering.

### 3.3 Submission / completion cycle

Fill SQE for `IORING_OP_READ` (opcode 22, kernel ≥ 5.6; single contiguous buffer) or `IORING_OP_READV` (opcode 1, scatter/gather): set `opcode`, `fd`, `off`, `addr` (buffer address via `MemorySegment.ofBuffer(bb).address()`), `len`, and a unique `user_data` correlation token. Push index into `sq_array`, bump `sq_tail` (release), then `io_uring_enter(to_submit=n)`. Reap: read `cq_tail` (acquire), for each CQE read `user_data` + `res` (`res<0` ⇒ `-errno`), look up the pending request in a `LongObjectHashMap<PendingRead>`, complete its future, advance `cq_head` (release).

### 3.4 Registered buffers & fixed files (optimization, later phase)

- `IORING_REGISTER_BUFFERS` + `IORING_OP_READ_FIXED` (opcode 21): pin `BufferPool` macro-chunks (8 MiB direct slabs, `BufferPool.java`) once; SQEs reference a `buf_index` instead of an address — removes per-I/O page pinning. Charges `RLIMIT_MEMLOCK`; handle `ENOMEM` by falling back to unregistered I/O.
- `IORING_REGISTER_FILES` (kernel ≥ 5.5) + `IOSQE_FIXED_FILE`: register SSTable fds, use a ring slot instead of the raw fd.
- **O_DIRECT alignment:** buffer address, offset, and length must be block-aligned (512 or 4096). Reuse existing helpers: `FileUtils.getBlockSize()` (`FileUtils.java:789`), Agrona `BufferUtil.allocateDirectAligned(cap, align)` / `BitUtil.align(...)` (already used by `DirectCompressedSequentialWriter`, `DirectThreadLocalReadAheadBuffer`). Note the `BufferPool` allocation unit is 2 KiB — 512-aligned but **not** guaranteed 4096-aligned; DMA buffers for O_DIRECT should use `allocateDirectAligned(chunkSize, blockSize)` rather than raw pool slices, or the pool must be taught a 4096-aligned unit.

### 3.5 Raw fd extraction (already solved)

`NativeLibrary.getfd(FileChannel)` / `getfd(FileDescriptor)` (`NativeLibrary.java:379,398`) read `sun.nio.ch.FileChannelImpl.fd` / `FileDescriptor.fd` via reflection; `ChannelProxy.getFileDescriptor()` (`:218-220`) is the public accessor. Works on JDK 25 given `--add-opens java.base/sun.nio.ch=ALL-UNNAMED` (already in `jvm25-server.options:110`).

### 3.6 Kernel & runtime requirements

- **Minimum kernel 5.6** (for `IORING_OP_READ`). Ubuntu 22.04 (5.15) ✓, RHEL 9 (5.14) ✓; Ubuntu 20.04 (5.4) ✗.
- Probe at startup: `io_uring_setup(1, &params)`; treat `ENOSYS` (not compiled/too old) and `EPERM` (seccomp, e.g. default Docker profile blocks io_uring on ≥5.10) as "unavailable → fall back." Record `params.features`.
- **`--enable-native-access=ALL-UNNAMED` must be added to `conf/jvm25-server.options`** (and test JVM args) — it is *not* present today and is mandatory for FFM downcalls. `JDK25_SUPPORT_PLAN.md` lists it as optional for JNI-warning suppression; for this feature it is required.

### 3.7 Threading model of the ring

**One ring per I/O carrier thread** (no cross-thread SQ contention), submission + completion driven on that thread; foreign submitters hand off via a queue. Default **non-SQPOLL** (SQPOLL burns a full CPU per ring and needs `CAP_SYS_ADMIN`/`CAP_SYS_NICE` pre-5.12); expose `io_uring_sqpoll` as an opt-in. Model the poller on the existing `executorFactory().infiniteLoop("...", task, UNSAFE)` pattern (used by `BufferPool.localPoolCleaner`) and the epoll `EventLoopGroup` lifecycle in `SocketFactory.java:107-168,283-288`.

### 3.8 Binding class breakdown

| Class | Responsibility |
|---|---|
| `IoUringAvailability` | Startup probe + feature/kernel detection; mirrors `NativeLibrary.isAvailable()`. |
| `IoUringSyscalls` | FFM downcall handles for setup/enter/register + mmap/munmap; per-arch syscall numbers; `captureCallState("errno")`. |
| `IoUringLayouts` | All `MemoryLayout`/`VarHandle`/opcode/flag constants. |
| `IoUringRing` | One ring: ring fd + mmap'd SQ/CQ/SQE segments (`Arena.ofConfined`); `AutoCloseable`. |
| `IoUringSQRing` / `IoUringCQRing` | SQE fill + submit; CQE drain. Single-threaded per ring. |
| `IoUringReadRequest` | Per-read state: `user_data`, buffer segment, `CompletableFuture<Integer>`, fd, offset, len; pooled. |
| `IoUringEventLoop` | Owns a ring + in-flight map; `submit(fd,off,len,buf) → CompletableFuture<Integer>`; poller loop; transparent fallback to `ChannelProxy.read` when unavailable. |
| `IoUringBufferRegistrar` | Registered buffers/files management (later phase). |
| `IoUringChannelProxy` | `ChannelProxy` subtype: `read()` (sync-style, `future.get()` — safe on a virtual thread) + `readAsync()`. |
| `IoUringManager` | Singleton lifecycle: start after startup checks, shutdown via pre-shutdown hook; JMX metrics. |

---

## 4. Component 2 — async `ChunkCache` & buffer lifecycle

### 4.1 Cache: `LoadingCache` → `AsyncLoadingCache`

Caffeine **3.1.8** (in `lib/`) supports `AsyncLoadingCache` / `AsyncCacheLoader.asyncLoad(key, executor) → CompletableFuture<V>` (`buildAsync(...)`). Switching gives **free in-flight read dedup**: while a chunk's future is in flight, concurrent readers of the same key subscribe to the same future — no duplicate device read. `asyncLoad` allocates a buffer from `BufferPool`, submits to the ring, and completes the future in the CQE handler (freeing the buffer on error). Weigher / removal-listener / refcount semantics are unchanged.

The current TOCTOU retry loop in `CachingRebufferer.rebuffer` (`ChunkCache.java:234-251`, `do { buf = cache.get(key).reference(); } while (buf == null)`) becomes a recursive async composition (`thenCompose` re-get on `reference()==null`).

### 4.2 Buffer lifecycle across threads

`ChunkCache.Buffer` refcount is an `AtomicInteger` with a CAS `reference()`/`release()` (`ChunkCache.java:98-144`) — already thread-safe. The three-thread pipeline (submit → completion → continuation) is safe because:

- `BufferPool.put()` (`BufferPool.java:245`) routes to the *calling* thread's `LocalPool`; foreign-thread release is **safe** (chunk `free()` is a CAS on `freeSlots`) but bypasses the fast local-queue recycle — the chunk goes via `GlobalPool.tryRecycle()`. Correct, marginally less efficient.
- The `Buffer` is published through the `CompletableFuture` happens-before edge; the consumer sees a fully written `ByteBuffer`.
- **Invariant to enforce:** the buffer must not return to the pool until *after* `future.complete()` — guaranteed because the `Buffer` wrapper is created in the CQE handler and holds the sole reference.

**Open concern:** Caffeine maintenance currently runs on `ImmediateExecutor` (`ChunkCache.java:152`); with async futures, eviction `onRemoval → buffer.release()` may execute on the completion/poller thread. Give Caffeine a small dedicated executor to avoid doing pool work on the ring poller.

### 4.3 Alignment & `BufferPool` interaction

See §3.4. Reuse `FileUtils.getBlockSize()` + Agrona `allocateDirectAligned`/`align`. Chunk sizes (65536) are already 4096-multiples, so **offset** alignment is satisfied by the existing `alignmentMask = -chunkSize`; **length** must be rounded up before submit (pattern: `DirectThreadLocalReadAheadBuffer.java:45`) and the buffer limit trimmed to the real length after completion; **address** alignment needs the aligned allocator for the DMA buffer.

---

## 5. Component 3 — config, startup gating, threading, lifecycle, fallback

### 5.1 Config plumbing

- **Enum:** add `io_uring` to `Config.DiskAccessMode` (`Config.java:1355-1367`), with a Javadoc note (like `direct`'s) pointing at the new startup check.
- **`DatabaseDescriptor` resolution** (`:674-708`): add an `io_uring` branch to `disk_access_mode` handling (set `conf.disk_access_mode` and `indexAccessMode`); add an `else if (io_uring)` branch to `compaction_read_disk_access_mode` (`:695-708`). Leave `commitlog_disk_access_mode` (`:1842-1906`) and `background_write_disk_access_mode` (`:3498-3525`) **unsupported** for io_uring — this is a read-path feature.
- **New `Config` fields** (not enum values): `io_uring_queue_depth` (default 256), `io_uring_poller_threads` (default 1), `io_uring_sqpoll` (default false), `io_uring_fallback_on_unavailable` (default true). Surface in `cassandra.yaml` near the existing `compaction_read_disk_access_mode` block (`cassandra.yaml:691-713`).
- Validate `io_uring_queue_depth >= concurrent_reads` (warn otherwise).

### 5.2 Startup gating + fallback

Add `checkIoUringAvailability` to `StartupChecks.DEFAULT_TESTS` (`StartupChecks.java:123-141`), modeled on `checkDirectIOSupport` (`:872-927`) and `checkKernelBug1057843` (`:244-302`):

1. Skip unless a resolved mode is `io_uring`.
2. `if (!FBUtilities.isLinux) …` — Linux only.
3. Kernel version via `FBUtilities.getKernelVersion()` (OSHI `Semver`); require ≥ 5.6 using a `Range<Semver>` (same pattern as the kernel-bug check).
4. Probe FFM availability (`Arena.ofConfined()` / a trivial downcall) and an `io_uring_setup(1,…)` probe against a data dir.
5. **Fallback:** if `io_uring_fallback_on_unavailable`, `logger.warn` and `DatabaseDescriptor.setDiskAccessMode(standard)` instead of throwing (mirrors `NativeTransportService.useEpoll()` → NIO fallback). Otherwise throw `StartupException` (`ERR_WRONG_MACHINE_STATE` / `ERR_WRONG_DISK_STATE` / `ERR_WRONG_CONFIG`).
6. Update `checkKernelBug1057843` per the `Config.java:1364` contract if io_uring O_DIRECT reads are subject to the same ext4 6.1.64–6.1.66 issue.

### 5.3 Read-stage integration (with virtual threads)

`Stage.READ` is a `SEPExecutor` sized by `concurrent_reads` (`Stage.java:46`). Recommended model:

- Run local-read execution (the `LocalReadRunnable` body, `StorageProxy.java:2740`) on **virtual threads** whose carriers are a bounded pool (size ~`concurrent_reads` or CPU-count). The read code stays synchronous; blocking on the ring completion future unmounts the carrier (§2 Strategy C).
- **One `IoUringEventLoop` ring per carrier**, plus a dedicated **completion poller** `infiniteLoop` thread (naming via `NamedThreadFactory`, e.g. `IoUring-CompletionPoller:1`; JMX via `ThreadPoolMetrics`).
- Submission from a read carrier → SQE into that carrier's ring; poller reaps CQEs and completes futures.
- Keep buffer allocation on the bounded carrier layer, **not** per virtual thread, to avoid `FastThreadLocal` `LocalPool` thrash (§2 caveat).

### 5.4 Backpressure

Today the read path is bounded only by `concurrent_reads` work permits (`SEPExecutor.java:59-79`); the task queue is unbounded. Add a per-ring `Semaphore(io_uring_queue_depth)`: acquire before submit, release on CQE. `io_uring_enter` returning `-EBUSY` (SQ full) triggers wait/retry. Effective in-flight = `min(concurrent_reads × per-ring-depth, io_uring_queue_depth)`.

### 5.5 Lifecycle

- **Init:** `IoUringManager.instance.start()` in `CassandraDaemon.setup()` **after** `runStartupChecks()` (`~CassandraDaemon.java:275`) and after `BufferPools` class-load, before schema/SSTable access. Fail-fast so a bad probe aborts before allocating rings.
- **Shutdown:** register `StorageService.instance.addPreShutdownHook(() -> IoUringManager.instance.shutdown())` (`StorageService.java:4091`). Order: stop accepting new reads → drain in-flight completions → interrupt poller → `IoUringRing.close()` (munmap + close fd) → unregister JMX. Mirror `SocketFactory.shutdownNow()` (`:283-288`).

### 5.6 Touch-point summary

| Area | File:line | Action |
|---|---|---|
| Enum | `Config.java:1355-1367` | add `io_uring` |
| SSTable resolution | `DatabaseDescriptor.java:675-692` | add branch |
| Compaction-read resolution | `DatabaseDescriptor.java:695-708` | add branch |
| New config fields | `Config.java` (~1355), `cassandra.yaml:691-713` | queue depth / poller / sqpoll / fallback |
| Startup check | `StartupChecks.java:123-141` + new method | availability + fallback |
| FileHandle mode dispatch | `FileHandle.java` Builder `complete()` / `ioMode()` (~`:450-525`) | route `io_uring` → `IoUringChunkReader` / `IoUringChannelProxy` |
| ChunkReader impl | new `IoUringChunkReader` implementing `ChunkReader` | wraps ring submit; drops into `ChunkCache.wrap(...)` (`ChunkCache.java:187-197`) |
| JVM options | `conf/jvm25-server.options` | add `--enable-native-access=ALL-UNNAMED` |
| Init / shutdown | `CassandraDaemon.java:~275`, `StorageService.java:4091` | start / pre-shutdown hook |

---

## 6. Phased delivery

Each phase is independently mergeable and revertible; later phases are gated on data from earlier ones.

### Phase 0 — FFM binding + isolated microbenchmarks *(no read-path changes)*
- Implement `IoUringSyscalls`, `IoUringLayouts`, `IoUringRing`, `IoUringEventLoop`, `IoUringAvailability` (§3).
- Add `--enable-native-access` to JVM options; `checkIoUringAvailability` skeleton.
- **Deliverable:** JMH `IoUringChunkReaderBench` + `IoUringBatchedReadThroughputBench` comparing **pread vs O_DIRECT vs io_uring** at queue depths 1→128, cache-cold (§9).
- **GATE 0:** io_uring must beat `direct`/O_DIRECT on cache-cold reads at realistic queue depth (≥16) on NVMe, and not regress single-read latency badly. **If not, stop.**

### Phase 1 — async ChunkReader at the single seam, via virtual threads
- `IoUringChunkReader` + `IoUringChannelProxy`; wire `disk_access_mode: io_uring` through config/resolution/`FileHandle`.
- Run local reads on virtual threads (bounded carriers); block on ring completion (Strategy C). No CPS.
- `AsyncLoadingCache` conversion of `ChunkCache` for in-flight dedup (§4), or keep sync loader initially and rely on Loom unmount (measure both).
- Full correctness suite (§8), parameterized across access modes; dtests; Harry differential oracle.
- **GATE 1:** correctness parity with `standard`/`mmap` (Harry + parameterized unit + dtest green); macro read-heavy p99 improvement on cache-cold NVMe; **no regression** on hot-cache workloads.

### Phase 2 — registered buffers/files + scan/compaction readahead
- `IoUringBufferRegistrar` (registered buffers/fixed files, §3.4); `compaction_read_disk_access_mode: io_uring`.
- Strategy B readahead for scans/compaction.
- **GATE 2:** compaction throughput / scan MB/s improvement with registered buffers vs Phase 1.

### Phase 3 *(optional, only if a proven bottleneck remains)* — CPS hot path
- Strategy A async iterators for the point-read hot path. Highest cost; only if Phase 1/2 data shows the virtual-thread suspension overhead is the bottleneck.

---

## 7. Correctness test plan

### 7.1 Unit tests (new)
Mirror existing patterns (`RandomAccessReaderTest`'s `FakeFileChannel`, `DirectCompressedChunkReaderTest`'s `qt()` property style + `FakeLargeFileChannel`):

| Class | Covers |
|---|---|
| `IoUringBindingTest` | FFM setup/enter/exit round-trip; param validation; ENOSYS via mocked FFM. |
| `IoUringChunkReaderTest` | `qt()` across all compressors × chunk sizes; partial-fill retry; EOF; >4 GiB offsets; deterministic `FakeIoUringRing`. |
| `IoUringSimpleChunkReaderTest` | uncompressed read/seek/EOF via `FileHandle.Builder.withDiskAccessMode(io_uring)`. |
| `IoUringCqeErrorTest` | table-driven negative `res` → `IOException`/`FSReadError`; reuse `DiskFailurePolicyTest` `@Parameterized` scaffold (`:54-80`). |
| `IoUringAlignmentTest` | block-equal / sub-block / straddling / multi-block; `FileUtils.getBlockSize`. |
| `IoUringRingBackpressureTest` | `EBUSY`/ring-full retry; in-flight limit; no dropped requests. |
| `IoUringFallbackTest` | ENOSYS → transparent fallback to `standard`; assert `DatabaseDescriptor.getDiskAccessMode()`. |

### 7.2 Parameterize existing suites across access modes
Use the `@RunWith(Parameterized.class)` pattern from `CQLSSTableWriterDaemonTest.java:39-49`. Add `io_uring` to: `CompressedRandomAccessReaderTest` (hook at `:188` `withDiskAccessMode`), `RandomAccessReaderTest`, `SSTableReaderDataReaderTest`. Add `IoUringTestUtils.withIoUringReads(...)` next to `DirectIoTestUtils.java`. Add `DatabaseDescriptorTest.testIoUringDiskAccessMode` for resolution.

### 7.3 In-JVM dtests
`.set("disk_access_mode","io_uring")` (pattern: `RepairErrorsTest.java:96`, `InstanceConfig`): `IoUringRepairTest` (no `FSReadError` in logs), `IoUringCompactionReadTest` (row correctness after major compaction), `IoUringStreamingTest`, `IoUringDiskFailurePolicyTest` (inject `FSReadError`, verify `disk_failure_policy=stop`). Need a property to bypass the kernel check in dtests (like `IGNORE_KERNEL_BUG_1057843_CHECK`).

### 7.4 Harry differential oracle
`HarryIoUringReadTest` (pattern: `HarryCompactionTest.java:152-232`): drive `HistoryBuilder` + `QuiescentChecker`; run identical history under `io_uring` and `standard`; assert identical model states. Any divergence = bug.

### 7.5 Fault-injection matrix
Primary tool: a programmable `FakeIoUringRing` (returns partial fills, specific `-errno`, ring-full). Plus `ListenableFileSystem.onPreRead/onPostRead` (`:330-356`) and Byteman `Injections` (`inject/Injections.java`). Cover: short read, `EAGAIN`/ring-full, negative CQE (`EIO`), `EINTR` retry, misalignment, kernel-unavailable fallback, teardown-with-in-flight, CRC mismatch (→ `CorruptSSTableException`, as `CompressedRandomAccessReaderTest:241`).

### 7.6 Simulator
io_uring completions arrive on a kernel/poller thread outside the deterministic scheduler; virtual-thread unmount + async completion break the simulator's cooperative model. **Keep the simulator on `standard`** (enforced at `ClusterSimulation.java:885`); add an assertion that `io_uring` is never requested under simulation, and a property to force fallback. Simulator coverage of io_uring is out of scope (would need a `SimulatedIoUringRing` delivering completions via `InterceptorOfConsequences`).

---

## 8. Performance test plan

Build via `ant microbench` (JMH 1.37, `.build/build-bench.xml`); `-Dbenchmark.name=…`, `-Djmh.args="…"`; profiling via `microbench-with-profiler`.

### 8.1 Microbenchmarks (new)
- `IoUringChunkReaderBench` — single-read latency (`Mode.SampleTime`, p50/p99/p999) at `accessMode ∈ {standard,direct,io_uring}` × chunkSize {4,16,64,256 KiB}; `@Setup(Level.Invocation)` calls `NativeLibrary.trySkipCache` to force misses.
- `IoUringBatchedReadThroughputBench` — throughput vs queue depth {1,4,8,16,32,64,128}; the benchmark that exposes async batching. **This is the Gate 0 benchmark.**
- `DiskAccessModeSSTableReadBench` (extends `SSTableAbstractBench`) — sequential scan MB/s + random point-read latency; `diskAccessMode` × `fileCacheEnabled`.
- `ChunkCacheMissLatencyBench` — proper-JMH version of `CachingBenchTest` (fixes its FIXME `:58`); reports `ChunkCacheMetrics.missLatency` p99.
- `CompactionReadModeBench` (extends `CompactionBench`) — compaction MB/s at `compaction_read_disk_access_mode ∈ {standard,direct,io_uring}`.

### 8.2 Macro (cassandra-stress)
- Read-heavy cache-cold: `read n=<10× RAM> -rate threads=64`, `-Dcassandra.file_cache_enabled=false`.
- Mixed 95/5: `mixed ratio(write=1,read=19) -rate threads=128`.
- Wide-partition scan: profile YAML with wide clustering + `token_range_queries` `page_size`.
- Compaction throughput: bulk write then `nodetool compact`, watch `iostat -x`.

### 8.3 Metrics & expected direction
`ChunkCache.MissLatency` p99 ↓; `Table.ReadLatency` p99/p999 ↓ (esp. high concurrency); `ClientRequest.Read.Timeouts` ↓ under saturation; `iostat avgqu-sz` ↑ (batching proof); `perf stat` pread syscalls ↓; `SSTablesPerReadHistogram` unchanged (correctness); hot-cache path unchanged (no-regression proof).

### 8.4 Matrix
`disk_access_mode {mmap,standard,io_uring}` × `compaction_read {standard,direct,io_uring}` × `file_cache_enabled {true,false}` × `concurrent_reads {8,32,64,128}` × queue depth {1..128} × chunk/payload {4..256 KiB} × cache-hit {0,~50,~95%} × storage {SATA SSD, NVMe} × compression {none, LZ4, Zstd}. **Critical cell:** `io_uring × file_cache_enabled=false × concurrent_reads=128 × NVMe`.

### 8.5 Forcing misses
Disable ChunkCache (`-Dcassandra.file_cache_enabled=false`, `ChunkCache.instance == null`); drop OS page cache per invocation via `NativeLibrary.trySkipCache(dataFile, 0, 0)` (`NativeLibrary.java:242-258`, pattern `SSTableReader.java:1764`) or dataset > RAM.

---

## 9. Risks, open questions, go/no-go gates

**Risks**
- **The win may not exist.** With ChunkCache + page cache absorbing hot reads, io_uring's benefit is confined to cache-cold/scan/compaction. Gate 0 exists to kill the project early if the microbench doesn't beat O_DIRECT.
- **Virtual-thread pinning / thread-local thrash** (§2 Strategy C caveats) — must be validated empirically, not assumed.
- **Platform fragility** — kernel ≥ 5.6, seccomp/container restrictions, io_uring CVE history. Mandatory optional-with-fallback (§5.2).
- **Correctness of a new critical-path reader** — mitigated by parameterized reuse of existing suites + Harry differential oracle.
- **Simulator blind spot** — io_uring path won't get deterministic simulator coverage (§7.6); leans harder on Harry + fault injection.
- **FFM maintenance surface** — first FFM usage in the tree; raw-syscall ABI is arch-specific and kernel-ABI-coupled.

**Open questions**
1. Sync `LoadingCache` + Loom unmount vs `AsyncLoadingCache` dedup — measure which wins in Phase 1.
2. Registered buffers vs pooled `BufferPool` slabs — `RLIMIT_MEMLOCK` budget and re-registration cost (Phase 2).
3. Carrier-pool sizing and buffer-ownership layer to avoid `FastThreadLocal` thrash.
4. Does io_uring + O_DIRECT hit the same ext4 6.1.64–6.1.66 bug as direct writes (`checkKernelBug1057843`)?
5. Ship the C shim (§3.1 Option B) or commit to pure-`syscall` FFM binding?

**Gates:** Gate 0 (microbench beats O_DIRECT at QD≥16 on NVMe) → Gate 1 (correctness parity + macro p99 win cache-cold, no hot-path regression) → Gate 2 (compaction/scan win from registered buffers). Phase 3 only on a proven residual bottleneck.

---

## Appendix — primary source references

Config/resolution: `Config.java:130,251,416,486,1355-1367`; `DatabaseDescriptor.java:674-708,1842-1906,3498-3525`.
Read path: `ChannelProxy.java:69-80,169-180,218-220`; `SimpleChunkReader.java:37-43`; `Rebufferer.java:36,71`; `BufferManagingRebufferer.java:77-82`; `RandomAccessReader.java:79-88`; `Walker.java:94-109`; `BigTableReader.java:251-403`; `BtiTableReader.java:235-294`; `AbstractSSTableIterator.java:76`; `SinglePartitionReadCommand.java:556`; `ReadCommand.java:506-534`; `StorageProxy.java:2649-2786`.
Cache/buffers: `ChunkCache.java:146-174,187-197,234-251`; `BufferPool.java:210,245,1099`.
Native/FFM: `NativeLibrary.java:379-413,242-258`; `NativeLibraryLinux.java:56`; `jvm25-server.options:99-111`; `JDK25_SUPPORT_PLAN.md`.
Threading/lifecycle: `Stage.java:46`; `SEPExecutor.java:59-79`; `SocketFactory.java:107-168,283-288`; `NativeTransportService` `useEpoll`; `CassandraDaemon.java:~275`; `StorageService.java:4091`.
Startup: `StartupChecks.java:123-141,244-302,872-927`.
Tests: `RandomAccessReaderTest.java`; `DirectCompressedChunkReaderTest.java`; `CompressedRandomAccessReaderTest.java:62-100,188`; `CQLSSTableWriterDaemonTest.java:39-49`; `DiskFailurePolicyTest.java:54-80`; `ListenableFileSystem.java:330-356`; `HarryCompactionTest.java:152-232`; `ClusterSimulation.java:885`.
Bench: `.build/build-bench.xml`; `SSTableAbstractBench.java`; `CachingBenchTest.java:58,202-344`; `ChunkCacheMetrics.java:40,50`; `TableMetrics.java:120,124`.
