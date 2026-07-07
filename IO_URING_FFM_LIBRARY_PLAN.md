# Implementation Plan: A Standalone Panama/FFM `io_uring` Library for Apache Cassandra (+ `disk_access_mode=io_uring`)

**Status:** Design proposal / implementation-grade plan.
**Primary deliverable:** a **fully-featured, self-contained, future-extractable** `io_uring` binding for Linux, written entirely with the JDK 25 Foreign Function & Memory (FFM / "Panama") API — *no JNI, no C shim, no `liburing` dependency*. It is designed to stand alone as if it were a general-purpose library (full opcode surface, registration, provided buffers, network ops) with **zero hard Cassandra dependencies** behind a small SPI.
**Driving consumer:** a new `disk_access_mode=io_uring` for Cassandra's SSTable read path, added as a thin, revertible adapter on top of the library.
**Branch context:** `cscotta/jdk25` (JDK 25 support landed as commit `52ed036ac8`).

> **Relationship to `IO_URING_DISK_ACCESS_MODE_PLAN.md`:** that earlier draft is read-path-integration-focused and treats the FFM binding as one sub-component (§3). This document **supersedes and reframes** it around the library as the primary artifact, and **corrects several factual errors** in it (see §0.2). The earlier draft's read-path strategy analysis (Strategy A/B/C) is folded into §12 here.

> **Provenance:** the ABI, FFM, and codebase facts below were **empirically verified on the development host** — aarch64, kernel `7.0.13-400.asahi.fc44.aarch64+16k`, OpenJDK `25.0.3`, glibc `2.43`, `liburing 2.13`, header `/usr/include/linux/io_uring.h` (2025-06-23). Struct sizes/offsets were confirmed by compiling `offsetof`/`sizeof` probes; FFM behaviors were confirmed by running probe programs on 25.0.3. Facts not so verified are marked *(from kernel/JDK docs)*.

---

## 0. Executive summary

### 0.1 The shape of the work

Cassandra's SSTable read path is **synchronous and thread-per-request**: a `ReadStage` thread (`concurrent_reads=32`, `Config.java:251`) calls `Rebufferer.rebuffer(pos)` (`Rebufferer.java:36`), which ultimately blocks on `FileChannel.read(buf, pos)` (`ChannelProxy.java:169-180`) or reads from an `mmap`. io_uring's value is **asynchronous, batched** device I/O; realizing it means issuing reads to a ring and completing them from CQEs instead of blocking a thread per read.

The plan builds **bottom-up and low-risk**:

1. **Build and prove the standalone FFM `io_uring` library in isolation** (microbenchmarks + a kernel-free fake ring) *before* touching the read path. This is the bulk of the engineering and carries no Cassandra risk.
2. **Introduce async at exactly one seam** — the `ChunkReader`/`ChunkCache` layer — and absorb the asynchrony with **JDK 25 virtual-thread mounting** (Loom), so the deserialization stack stays textually synchronous and no continuation-passing rewrite is required.
3. **Measure at every phase, but do not gate on the isolated benchmark.** The P0 microbenchmark (§15) is still built and run, and reported honestly — io_uring vs `standard`/O_DIRECT/mmap at realistic queue depths on cache-cold reads. Its results are a **baseline and tuning input** (they inform *which paths* enable io_uring/O_DIRECT by default), **not a go/no-go gate**: the work proceeds regardless.

> **Reality check that bounds the whole effort:** io_uring only helps **cache misses that reach the device.** Hot reads are served from the off-heap `ChunkCache` or the OS page cache and issue no syscall. The win concentrates in (a) high-concurrency cache-cold point reads, (b) large scans / compaction reads, (c) tail latency under device saturation on NVMe. The hot-cache path **must be a no-op / no-regression.** ScyllaDB measured only ~5% of io_uring over well-tuned linux-aio; the prior-art library JUring's own JMH shows `FileChannel` *beating* io_uring at ≥20 writer threads / 64 KiB. **The isolated microbenchmark must therefore be run and reported honestly** — comparing against `standard`/O_DIRECT/mmap at realistic queue depths — so the results steer where io_uring is enabled by default, even though they do not gate whether the work proceeds.

### 0.2 Corrections this plan absorbs (verified; the earlier draft is wrong on these)

- **`--enable-native-access=ALL-UNNAMED` is already present** at `conf/jvm25-server.options:115` (added by the JDK25 commit). The draft's "must be added / not present today" is stale. The real gaps are the **ant/test/JMH JVM args** (`build.xml`, absent everywhere) and, if a client tool ever uses FFM, `conf/jvm25-clients.options`.
- **`IORING_OP_READ_FIXED` is opcode 4**, not 21 (21 is `STATX`). `READ`=22, `READV`=1, `WRITE_FIXED`=5, `READV_FIXED`=60.
- **Kernel version compare uses `com.vdurmont.semver4j.Semver` (LOOSE) + Guava `Range`** (`StartupChecks.java:52-53`), **not OSHI**.
- **`ChannelProxy.getChannel()` does not exist** — use `ChannelProxy.getFileDescriptor()` (`ChannelProxy.java:218`).
- **`disk_access_mode: direct` is rejected today** (`DatabaseDescriptor.java:685-687` throws). `io_uring` is genuinely new top-level read plumbing, not a `direct` variant.
- **There is no `index_access_mode` yaml knob**; `indexAccessMode` is a derived private field (`DatabaseDescriptor.java:225`). The fallback-to-`standard` path must reset **both** `disk_access_mode` and `indexAccessMode`.
- **`DataComponent` is write-only**; the read data-file access mode is chosen in `SortedTableReaderLoadingBuilder.java:65`.
- **`FileHandle.ioMode()` has `default: throw new AssertionError`** (`FileHandle.java:467`) — a new enum value crashes at runtime unless a `case io_uring:` and a new `ChannelProxy.IOMode` constant are added.
- **`BufferPool` geometry:** `NORMAL_CHUNK_SIZE=128 KiB`, allocation unit `2 KiB`, `MACRO_CHUNK_SIZE=8 MiB` (the `:388` "1 MiB" comment is stale). For chunk-sized (≥4 KiB power-of-two) reads the pool already yields ≥4096-aligned direct buffers.
- **SQPOLL privilege boundary:** kernel **5.11 = `CAP_SYS_NICE`**, **5.13+ = none** (the draft's "pre-5.12" is imprecise).
- **The FFM package compiles only on JDK 25** (`java.lang.foreign` is preview on 21, absent on 17). Cassandra compiles with `-source/-target ${ant.java.version}` and **no `--release`**, and CI compiles on 11/17/21/25 → a **compile-exclusion + ServiceLoader boundary is mandatory, not stylistic.**

### 0.3 Firm architectural calls

| Decision | Call | Rationale |
|---|---|---|
| Binding mechanism | **Pure raw-`syscall` FFM.** No JNI, no C shim, no `liburing`. | The tree has *no* native/C build and *no* FFM yet; a shim would add a whole per-arch toolchain + binary-release process the project has never had. Unique among Java bindings except PanamaUring. |
| Op surface | **Full `prep_*` surface + full async ergonomics (storage AND network/multishot/provided-buffers) + registration + buf-rings** in the library. Cassandra wires the storage-read subset (`READ/READV/READ_FIXED/READV_FIXED/FSYNC` + `NOP`); write preps (`WRITE/WRITEV/FSYNC`) are present for future commitlog/flush consumers. | A genuinely general-purpose, standalone library — the async facade covers sockets/multishot/zero-copy too, not just the pread path. |
| Concurrency | **Thread-per-ring** (bounded pool), foreign submitters hand off via an MPSC queue, **virtual threads as the suspension vehicle.** Not shard-per-core; not ring-per-vthread. | Cassandra isn't share-nothing; the SQ is single-producer; millions of ephemeral vthreads would thrash `FastThreadLocal` buffer pools. |
| Read-path async | **Strategy C (virtual-thread mounting)** primary; readahead (B) for scans; a concrete **CPS (Strategy A) execution plan is written (Appendix C) for a future session**, not started now. | Collapses the historical blast radius; every phase revertible; CPS deferred but pre-designed. |
| Extraction | **In-tree behind an SPI now; extract to `modules/io-uring` later** (mirror the `accord` submodule) once the API stabilizes under load. | The SPI boundary makes extraction a mechanical move rather than a rewrite. |
| v1 device access | **O_DIRECT supported from the start, but optional/configurable per path** (buffered also available). | Compaction reads — and future commitlog/flush **writes** — are the clearest O_DIRECT beneficiaries; the standard point-read path's benefit is less clear, so O_DIRECT is a per-path toggle, defaulting off for the standard read path. Alignment + the ext4 kernel-bug check are therefore in scope from v1 (§9 H12, §12.1). |

*(These reflect the confirmed decisions recorded in §18.)*

---

## 1. Verified environment & baseline facts

| Concern | Verified fact |
|---|---|
| Dev host | aarch64, kernel `7.0.13`, OpenJDK `25.0.3`, glibc `2.43`, liburing `2.13` |
| io_uring syscall numbers | `setup=425`, `enter=426`, `register=427` — **identical on x86-64 and aarch64** (generic ABI) |
| Struct sizes | `io_uring_params`=120 B (`features`@20, `sq_off`@40, `cq_off`@80); `io_sqring_offsets`/`io_cqring_offsets`=40 B; `io_uring_sqe`=64 B; `io_uring_cqe`=16 B; `iovec`=16 B; `__kernel_timespec`=16 B |
| SQE field offsets | `opcode`@0, `flags`@1, `ioprio`@2, `fd`@4, `off`@8, `addr`@16, `len`@24, `rw_flags`@28, `user_data`@32, `buf_index`@40 |
| CQE field offsets | `user_data`@0, `res`@8 (`<0` ⇒ `-errno`), `flags`@12 |
| FFM: syscall/mmap/munmap | all present in `Linker.nativeLinker().defaultLookup()` |
| FFM: errno | `Linker.Option.captureCallState("errno")`; `captureStateLayout()` has member `"errno"`; capture segment is the **leading** handle arg |
| FFM: ring barriers | layout/value `VarHandle` supports `getAcquire`/`setRelease` — the exact equivalents of liburing `smp_load_acquire`/`smp_store_release` **(this was the key open question; answer: yes)** |
| FFM: `structLayout` | does **not** auto-pad — every gap needs an explicit `paddingLayout(n)` |
| FFM: native-access default on 25 | `warn` (runs + one-time warning; module then marked enabled). `--illegal-native-access=deny` → `IllegalCallerException`. So the flag is **not strictly required** on 25, but is forward-compatible and silences the warning. |
| Compile constraint | `java.lang.foreign` is preview on JDK 21, absent on 17; compiles clean on 25 with no `--enable-preview`. CI compiles on 11/17/21/25. |
| 16 KiB pages | dev host uses a 16 KiB base page — any `mmap`-length rounding must use the **runtime** page size (`MemoryUtil.pageSize()`), never a hardcoded 4096. |

**Kernel floor: 5.6** (for `IORING_OP_READ`, opcode 22). This covers RHEL 9 (5.14) and Ubuntu 22.04 (5.15); it excludes Ubuntu 20.04 (5.4). `IORING_FEAT_NODROP` (≥5.5) and `IORING_FEAT_SINGLE_MMAP` (≥5.4) are therefore always present at the floor. Optional latency features `SINGLE_ISSUER` (6.0) / `DEFER_TASKRUN` (6.1) are feature-gated opt-ins.

> **Do not trust the compile host's header.** It exposes opcodes/features (through opcode 63, `IORING_FEAT_NO_IOWAIT`) that do not exist on RHEL 9 / Ubuntu 22.04. **Every optional path must be gated at runtime via `IORING_REGISTER_PROBE` and `params.features`, never by the compiled constant's presence.**

## 2. Design goals & non-goals

**Goals**
- **Standalone & general-purpose:** a complete io_uring surface (setup/teardown, full `prep_*`, submit/complete, registration, provided buffers, probe) usable for arbitrary async I/O, not just Cassandra reads.
- **Zero hard Cassandra dependencies** inside the core package — buffers, threads, logging, metrics, config, and error translation all behind a small SPI, so extraction to a standalone artifact is a mechanical move.
- **Pure FFM**, no native artifact, cross-arch (x86-64 + aarch64) by construction.
- **Memory-safe by runtime enforcement** (Rust bindings get this from the type system; we must hand-enforce it): the kernel may write into a buffer *after* Java thinks the op is done — an owning in-flight registry, memory-safe cancel, and drain-before-close are first-class invariants (§9).
- **Availability-probed and fallback-mandatory:** io_uring is the most-restricted-in-practice Linux subsystem; for a large fraction of deployments it will be unavailable by policy, so graceful fallback to `standard` is the *primary* path, not an edge case.
- **Testable without a kernel:** a deterministic `FakeIoUringRing` drives unit/property/fault tests on any OS/JDK and in the simulator.

**Non-goals**
- Not a Netty-style socket transport (that's Netty's job). Network opcodes are present for library completeness but not wired into Cassandra.
- Not a continuation-passing rewrite of the read stack in v1 (Strategy A is reserved).
- Not a replacement for `mmap`/`standard`/`direct` — an *additional*, opt-in read mode.
- The commitlog and write paths are out of scope (io_uring is a read-path feature here).

---

## 3. Library architecture

Package root **`org.apache.cassandra.io.uring`** is the extractable core (zero Cassandra deps). Cassandra adapters live in existing packages and are marked **[ADAPTER — outside core]**. Eight layers, bottom-up; each depends only on layers below it and on the SPI package.

### (a) Raw ABI / syscall layer — `o.a.c.io.uring.linux`
Depends on: JDK FFM only.

| Class | Responsibility |
|---|---|
| `Syscalls` | 3 fixed-arity downcall handles over libc variadic `syscall` (`firstVariadicArg(1)`, `captureCallState("errno")`); per-arch numbers 425/426/427; `invokeExact` only. |
| `LibC` | Handles for `mmap`/`munmap` (fixed 6-arg), `eventfd`/`eventfd_write`, `getrlimit`, and `pread` (golden-oracle probe only). |
| `Errno` | errno constants + `strerror`; reads the capture segment's `"errno"` member. |
| `Arch` | `os.arch` → syscall-number table (via injected property provider, not `System.getProperty`). Trivial today; localizes future arches. |

### (b) Memory-layout / constants layer — `o.a.c.io.uring.abi`
Depends on: FFM. Pure data; no syscalls. **Explicit padding everywhere.**

| Class | Responsibility |
|---|---|
| `Layouts` | `StructLayout`s for every ABI struct (§5.2), with **startup `byteSize`/`byteOffset` assertions** against ground truth (arch-layout guard). |
| `Opcodes` | `IORING_OP_*` 0..63. |
| `SetupFlags` / `FeatureFlags` / `EnterFlags` / `SqeFlags` / `CqeFlags` / `SqCqFlags` / `RegisterOps` / `MmapOffsets` | The flag/constant namespaces (§5.3). |
| `Sqe` | Thin accessor over a 64 B slice using offset-based `JAVA_*_UNALIGNED` VarHandles. |
| `Cqe` | Accessor over a 16 B slice. |

### (c) Safe ring layer — `o.a.c.io.uring.ring`
Depends on: (a),(b). **Single-threaded per ring.** Owns barrier correctness — the #1 corruption risk.

| Class | Responsibility |
|---|---|
| `Ring` | ring fd + mmap'd SQ/CQ/SQE segments (2 mmaps if `SINGLE_MMAP`, else 3), cached `sq_off`/`cq_off`, `Arena`, features record. `AutoCloseable` → munmap + close fd. |
| `SubmissionQueue` | `getSqe()` (local cursor, null when full), `flushSq()` (write `sq_array`, then **`setRelease`** ktail), `sqRingNeedsEnter()`, `sqReady()`, `sqSpaceLeft()`; `NO_SQARRAY` variant. |
| `CompletionQueue` | `peekCqe()`, `peekBatch(max, consumer)` / `forEachCqe`, `cqReady()` (**`getAcquire`** ktail − khead), `cqAdvance(n)` (**`setRelease`** khead), `cqRingNeedsFlush()`. |
| `RingBarriers` | The **only** place acquire/release modes are open-coded: `publishSqTail`/`loadSqHead`/`loadCqTail`/`advanceCqHead` + `acquireFence()` before an SQPOLL `kflags` read. "Never open-code these elsewhere." |
| `RingSubmitter` | `submit()`, `submitAndWait(n)`, `submitAndWaitTimeout(n,ts)` composing flush + needs-enter + `io_uring_enter`; EINTR/EAGAIN/EBUSY retry; EXT_ARG path when available. |
| `RingParams` | Immutable value: sq/cq entries, features, setup flags, mmap sizes. |

### (d) Op-prep helpers — `o.a.c.io.uring.op`
Depends on: (b). **Full opcode surface** (cheap field setters) — what makes the library standalone.

| Class | Responsibility |
|---|---|
| `Prep` | Base `prepRw(sqe,op,fd,addr,len,off)` that **zeroes the full 64 B first** (stale-union-byte leakage is the #1 hand-rolled-ring bug). Storage set **[v1-wired]**: `prepRead/Write/Readv/Writev/ReadFixed/WriteFixed/ReadvFixed/Fsync/Nop/Fallocate/Ftruncate/Fadvise`. |
| `PrepNet` | Network surface (present, not Cassandra-wired): accept/connect/send/recv/sendmsg/recvmsg/poll/shutdown, incl. multishot & zero-copy. |
| `PrepFs` | openat[2][direct]/close[direct]/statx/renameat/unlinkat/linkat/mkdirat/splice/tee. |
| `PrepCtl` | timeout[remove/update]/link_timeout/cancel(64)/files_update/msg_ring/provide_buffers/remove_buffers. |
| `TargetFixedFile` | Shared `file_index = (idx==ALLOC?ALLOC:idx+1)` logic for all `*_direct` variants. |

### (e) High-level async API — `o.a.c.io.uring.async`
Depends on: (c),(d),(f),(h). The facade + completion dispatch.

| Class | Responsibility |
|---|---|
| `IoUring` (facade) | `open(IoUringConfig)`, sync + async + batched submit, `features()`, `probe()`, registration delegation, `close()`. |
| `IoUringEventLoop` | Owns one `Ring` + one `InflightRegistry` + poller loop + **MPSC foreign-submitter hand-off** + eventfd wakeup. `submit(...) → CompletableFuture`. Submit+reap only on its owning thread. |
| `InflightRegistry` | **The keep-alive.** `LongObjectHashMap<PendingOp>` keyed by a **monotonic never-recycled 64-bit** `user_data`; owns the buffer reference until the terminal CQE. Drained-empty gate for teardown. |
| `PendingOp` | `user_data`, buffer (strong ref), `CompletableFuture<Integer>`, fd, off, len, submitted-len, short-read resubmit state; pooled. |
| `CompletionDispatcher` | Reaps CQEs; decodes `res` (`<0`⇒-errno exceptional; `0`⇒EOF; `0<res<len`⇒resubmit remainder); routes by `user_data`; `Reference.reachabilityFence(buffer)` after hand-off; batched reap. |
| `Backpressure` | Per-ring permit gate (= `queue_depth`) acquired before submit / released on CQE; CQ sized ≥2×SQ so overflow is impossible in steady state. |
| `IoUringException` | Library's typed error carrying errno; adapter translates to `FSReadError`/`CorruptSSTableException`. |

### (f) Registration / provided-buffers / fixed-files — `o.a.c.io.uring.reg`
Depends on: (a),(b),(c).

| Class | Responsibility |
|---|---|
| `BufferRegistrar` | `registerBuffers[Sparse/Update]/unregisterBuffers` (prefer `REGISTER_BUFFERS2`). `getrlimit(RLIMIT_MEMLOCK)` pre-check; `ENOMEM/EPERM` → caller falls back to unregistered `READ`. |
| `FileRegistrar` | `registerFiles[Sparse/Update]/unregisterFiles` (`REGISTER_FILES2`); stable slot→fd map; enables `IOSQE_FIXED_FILE`. |
| `ProbeTable` | `REGISTER_PROBE` → `Capabilities.supports(opcode)`. **Runtime opcode gating.** |
| `RingFdRegistrar` | `REGISTER_RING_FDS` → `ENTER_REGISTERED_RING` (latency opt; needs `FEAT_REG_REG_RING`). |
| `ProvidedBufferRing` | `setupBufRing/registerBufRing/BufRing.{init,add,advance,cqAdvance}/free` **[nice-to-have; socket/streaming, not the pread path]**. |
| `SyncCancel` | `REGISTER_SYNC_CANCEL` for shutdown drain. |

### (g) Availability / probe / feature detection — `o.a.c.io.uring.probe`
Depends on: (a),(b),(c).

| Class | Responsibility |
|---|---|
| `IoUringAvailability` | The hardened probe (§11): sysctl-file check → safe `setup(1)` → **exercise a real op** → record `Capabilities`. Cache once, never re-probe. |
| `Capabilities` | Immutable: available?, feature bitset, supported opcodes, memlock ceiling, single-mmap?, nodrop?, ext-arg?. |

### (h) SPI seams — `o.a.c.io.uring.spi`
Depends on: nothing. The extraction boundary. Core references **only** these.

| Interface / value | Cassandra impl **[ADAPTER]** | Standalone default |
|---|---|---|
| `BufferAllocator` — `acquire(size, align)/release/address` | wraps `BufferPools.forChunkCache()` + `MemoryUtil.getAddress` | Agrona `BufferUtil.allocateDirectAligned` |
| `UringLogger` | slf4j | slf4j |
| `UringStatsListener` — `onSubmit/onComplete(latency)/onFallback/inflightGauge/onOverflow` | forwards to `CassandraUringMetrics` | no-op / `LongAdder` |
| `RingThreadFactory` | `executorFactory().infiniteLoop(...,UNSAFE)` | plain daemon thread |
| `PermitGate` | `utils.concurrent.Semaphore` | `j.u.c.Semaphore` |
| `IoUringConfig` (value object) | populated from `Config`/`DatabaseDescriptor` | builder defaults |

### Cassandra adapter classes — **[OUTSIDE the core package]**

| Class | Package | Responsibility |
|---|---|---|
| `IoUringChunkReader` (compressed + uncompressed) | `io.util` | `implements ChunkReader`; submits to the event loop, `future.get()` on a virtual thread (Strategy C); translates errors. |
| `IoUringChannelProxy` | `io.util` | `ChannelProxy` subtype exposing the raw fd; optional `IOMode.IO_URING`. |
| `IoUringManager` | `service`/`io.util` | Singleton lifecycle: `start()` after `runStartupChecks()`, `shutdown()` via `StorageService.addPreShutdownHook`; owns the event-loop pool + SPI wiring + JMX. |
| SPI impls (`CassandraBufferAllocator`, `Slf4jUringLogger`, `CassandraUringMetrics`, `CassandraRingThreads`) | `io.util` | Bind core → `BufferPool`/slf4j/metrics/`ExecutorFactory`. |
| `checkIoUringAvailability` | `service.StartupChecks` | kernel-floor + fallback policy on top of `IoUringAvailability`. |
| enum + resolution + dispatch edits | `config`, `io.util` | `Config.DiskAccessMode.io_uring`; `DatabaseDescriptor` branches; `FileHandle` cases. |
| `META-INF/services/...AsyncReadProvider` | `src/resources` | ServiceLoader registration (present only in the JDK25 jar). |

---

## 4. Consumer context: how `disk_access_mode` works today

A short map of the subsystem the adapter plugs into (full seam list in §8).

**The enum** — `Config.DiskAccessMode` (`Config.java:1355-1367`): `auto, mmap, mmap_index_only, standard, legacy, direct`. Only `direct` carries a Javadoc, an explicit contract: *"When adding support for Direct I/O, update `StartupChecks#checkKernelBug1057843`."* A new device-touching mode must revisit that check.

**Resolution** — `DatabaseDescriptor.applySimpleConfig`:
- Main read mode `:674-693`: `auto`/`mmap_index_only` → data `standard` + index `mmap`; `legacy` → `hasLargeAddressSpace()? mmap : standard`; **`direct` → throws** (`:685-687`); else data+index = the mode. `indexAccessMode` is a **derived private field** (`:225`), not a yaml knob.
- Compaction-read `:695-708`: only `auto`/`direct` accepted; anything else throws `IllegalArgumentException`.
- Commitlog (`:1842-1912`) and background-write (`:3497-3525`) are **write paths** with closed allow-lists — out of scope; they will *reject* an `io_uring` value.

**Read call chain (bottom → top), verified:**

| # | Layer | file:line |
|---|---|---|
| 1 | `FileChannel.read(buf,pos)` / `MappedByteBuffer`; O_DIRECT open | `ChannelProxy.java:169-180`; `map` `194-204`; `openOptions` `69-80`; `IOMode {BUFFERED,DIRECT}` `47-51` |
| 2 | `ChunkReader.readChunk(pos,buf)` | iface `ChunkReader.java:39` (thread-safe req `:29`); `SimpleChunkReader.java:37-43`; `CompressedChunkReader.{Standard:399,Direct:296,Mmap:474}` |
| 3 | `Rebufferer.rebuffer(pos)` | iface `Rebufferer.java:36`; `BufferManagingRebufferer.java:76-82`; `MmapRebufferer.java:37-41`; `ChunkCache.CachingRebufferer` `232-252` (TOCTOU retry `239-241`) |
| 3a | cache load | `ChunkCache.load` `160-174`; `wrap` `187-190`; `LoadingCache` `:57`; `ImmediateExecutor` `:152`; `Buffer` refcount CAS `98-144` |
| 4 | `RandomAccessReader.reBufferAt(pos)` | `RandomAccessReader.java:78-88` (`@NotThreadSafe`) |
| 5-10 | trie walk / index / row iterator / `queryStorage` / `executeLocally` / `LocalReadRunnable` | `Walker.go:94`; `BigTableReader.getRowIndexEntry:251` / `BtiTableReader.getExactPosition:235`; `AbstractSSTableIterator:76`; `SinglePartitionReadCommand:556`; `ReadCommand:506`; `StorageProxy:2740` |

**Mode dispatch (the integration point):** `FileHandle.Builder.complete()` (`:472-536`) selects the rebufferer by mode; `ioMode()` (`:450-469`) maps mode→`ChannelProxy.IOMode` with a `default: throw AssertionError` at `:467`. Per-component mode flows via `IOOptions` (`defaultDiskAccessMode`/`indexDiskAccessMode`, `:28-39`) into `SortedTableReaderLoadingBuilder:65` (data), `IndexComponent:34` and `BtiTableReaderLoadingBuilder:209/224` (index). Per-scan compaction override: `SSTableReader.openDataReaderInternal:1448-1467`, `canReuseDfile:1469-1474`.

---

## 5. ABI reference (verified; the spec the `abi` layer implements)

### 5.1 The three syscalls (no glibc wrappers)

| Syscall | nr (x86-64 = aarch64) | C prototype |
|---|---|---|
| `io_uring_setup` | 425 | `int(u32 entries, io_uring_params *p)` → ring fd |
| `io_uring_enter` | 426 | `int(u32 fd, u32 to_submit, u32 min_complete, u32 flags, const void *arg, size_t argsz)` |
| `io_uring_register` | 427 | `int(u32 fd, u32 opcode, void *arg, u32 nr_args)` |

`io_uring_enter` is fixed 6-arg. Since 5.11 (`IORING_FEAT_EXT_ARG`), with `IORING_ENTER_EXT_ARG` the 5th/6th args become `io_uring_getevents_arg*` (24 B) + its size — a wait timeout without a timeout SQE. **enter flags:** `GETEVENTS=1<<0`, `SQ_WAKEUP=1<<1`, `SQ_WAIT=1<<2`, `EXT_ARG=1<<3`, `REGISTERED_RING=1<<4`. **errno:** `EAGAIN`/`EINTR` retry; `EBUSY` reap-then-retry; `ENOSYS`/`EPERM` ⇒ unavailable→fallback; `EFAULT`/`EINVAL` ⇒ binding bug.

### 5.2 Struct layouts (verified sizes/offsets; explicit `paddingLayout`)

```
io_uring_params (120B): sq_entries@0 cq_entries@4 flags@8 sq_thread_cpu@12
  sq_thread_idle@16 features@20 wq_fd@24 resv[3]@28 sq_off@40 cq_off@80
io_sqring_offsets (40B): head@0 tail@4 ring_mask@8 ring_entries@12 flags@16
  dropped@20 array@24 resv1@28 user_addr@32          # values are byte offsets into the SQ mmap
io_cqring_offsets (40B): head@0 tail@4 ring_mask@8 ring_entries@12 overflow@16
  cqes@20 flags@24 resv1@28 user_addr@32
io_uring_sqe (64B): opcode@0 flags@1 ioprio@2 fd@4 off@8 addr@16 len@24
  rw_flags@28 user_data@32 buf_index@40 personality@42 splice_fd_in@44 addr3@48 __pad2@56
io_uring_cqe (16B): user_data@0 res@8 flags@12          # res<0 ⇒ -errno; res==0 ⇒ EOF for reads
iovec (16B): iov_base@0 iov_len@8
__kernel_timespec (16B): tv_sec@0 tv_nsec@8             # NOT libc timespec (nsec is 64-bit)
```
Read `features`@20 after setup and cache it. Model the SQE union arm you use (READ) and pad to 64. Access with `JAVA_*_UNALIGNED` VarHandles at fixed offsets; **`fill(0)` the whole 64 B before filling** an SQE.

### 5.3 Opcodes & flags

- **Opcodes** (`IORING_OP_*`, enum ordinal = number): `NOP=0, READV=1, WRITE=2, FSYNC=3, READ_FIXED=4, WRITE_FIXED=5, POLL_ADD=6, POLL_REMOVE=7, … TIMEOUT=11, ACCEPT=13, ASYNC_CANCEL=14, LINK_TIMEOUT=15, … OPENAT=18, CLOSE=19, FILES_UPDATE=20, STATX=21, READ=22, WRITE=23, FADVISE=24, … SEND=26, RECV=27, … PROVIDE_BUFFERS=31, … RENAMEAT=35, … MSG_RING=40, … SEND_ZC=47, READ_MULTISHOT=49, … FTRUNCATE=55, … READV_FIXED=60, WRITEV_FIXED=61`. **Cassandra needs `READ(22)`, `READV(1)`, `READ_FIXED(4)`, `READV_FIXED(60)`, `FSYNC(3)`, `NOP(0)`.** Do not trust ordinals ≥34 to exist — probe.
- **SETUP flags:** baseline `0`. Opt-in on ≥6.1: `SINGLE_ISSUER(1<<12)|DEFER_TASKRUN(1<<13)` (big latency win for one-poller model; absent on RHEL 9/Ubuntu 22.04). `CQSIZE(1<<3)` to size CQ ≥2×SQ. `NO_SQARRAY(1<<16)` on ≥6.6 simplifies the SQ. `SQPOLL(1<<1)` opt-in only.
- **FEATURE flags** (`params.features`): `SINGLE_MMAP(1<<0, ≥5.4)`, `NODROP(1<<1, ≥5.5)`, `SUBMIT_STABLE(1<<2)`, `RW_CUR_POS(1<<3)`, `EXT_ARG(1<<8, ≥5.11)`, `RSRC_TAGS(1<<10)`, `CQE_SKIP(1<<11)`, `REG_REG_RING(1<<13, ≥6.3)`. Gate optional paths on these.
- **SQE flags** (`IOSQE_*`): `FIXED_FILE(1<<0)`, `IO_DRAIN(1<<1)`, `IO_LINK(1<<2)`, `IO_HARDLINK(1<<3)`, `ASYNC(1<<4)`, `BUFFER_SELECT(1<<5)`, `CQE_SKIP_SUCCESS(1<<6)`. **Cassandra reads: submit unlinked, no drain, one `user_data` each, reap out-of-order.** Links only for ordered readahead if ever needed.
- **CQE flags** (`IORING_CQE_F_*`): `BUFFER(1<<0)` (buffer id = `flags >> 16`), `MORE(1<<1)` (multishot — more CQEs coming, don't retire), `SOCK_NONEMPTY(1<<2)`, `NOTIF(1<<3)`.
- **SQ flags** (`kflags`): `NEED_WAKEUP(1<<0)` (SQPOLL parked), `CQ_OVERFLOW(1<<1)` (flush via `enter(GETEVENTS)`), `TASKRUN(1<<2)`.
- **REGISTER ops:** `BUFFERS=0, UNREGISTER_BUFFERS=1, FILES=2, EVENTFD=4, PROBE=8, ENABLE_RINGS=12, FILES2=13, BUFFERS2=15, RING_FDS=20, PBUF_RING=22, SYNC_CANCEL=24`; `USE_REGISTERED_RING=1U<<31`.

### 5.4 mmap layout & memory barriers (the correctness core)

**Offsets:** `SQ_RING=0x0`, `CQ_RING=0x8000000`, `SQES=0x10000000`, `PBUF_RING=0x80000000`. All maps: `PROT_READ|PROT_WRITE`, `MAP_SHARED|MAP_POPULATE`, fd=ring fd. With `SINGLE_MMAP` (always at our floor): **2 mmaps** (shared SQ+CQ ring, sized `max(sq_bytes,cq_bytes)`, + SQES); else 3. Sizes: SQ ring = `sq_off.array + sq_entries*4`; CQ ring = `cq_off.cqes + cq_entries*16`; SQES = `sq_entries*64`.

**The `sq_array` indirection:** to submit SQE pool-slot `i`, write `i` into `sq_array[sq_tail & mask]`, *then* advance `sq_tail`. (Single-threaded flow reuses slot = `tail & mask`.) `NO_SQARRAY` (≥6.6) removes this table.

**Barrier requirements (mirror liburing `smp_load_acquire`/`smp_store_release`; encoded only in `RingBarriers`):**

| Field | Owner | Access | Barrier |
|---|---|---|---|
| SQ `tail` | you | store after filling SQE + array | **`setRelease`** |
| SQ `head` | kernel | load (free space) | **`getAcquire`** |
| SQ `flags` | kernel | load (`NEED_WAKEUP`/overflow) | `acquireFence()` then plain, under SQPOLL |
| CQ `tail` | kernel | load (new CQEs) | **`getAcquire`** |
| CQ `head` | you | store after consuming | **`setRelease`** |

Getting these four points wrong = silent corruption or lost completions. **Never use plain `get`/`set` for shared counters; never `getVolatile` (imposes an unneeded StoreLoad fence — matters on aarch64).**

### 5.5 The internal submit/reap contract (reimplement exactly)

Cached local longs (not shared): `sqeHead`, `sqeTail`. Shared (mmap'd, acquire/release): `khead/ktail/kflags/array` (SQ), `khead/ktail` (CQ).

- `getSqe`: if `sqeTail+1 - sqeHead <= entries` return `&sqes[sqeTail & mask]`, `sqeTail++`; else null (must submit first).
- `flushSq`: `to_submit = sqeTail - sqeHead`; write `sq_array[...]` for each pending; **release-store** `ktail += to_submit` (single publication point). Skip array under `NO_SQARRAY`.
- `sqRingNeedsEnter`: non-SQPOLL → always true. SQPOLL → `acquireFence`; read `kflags`; if `NEED_WAKEUP` set `flags|=SQ_WAKEUP` and true; else false (skip the syscall).
- `cqRingNeedsFlush`: `kflags & (CQ_OVERFLOW|TASKRUN)`.
- `submit`: `n=flushSq()`; if `sqRingNeedsEnter(n) || (waitNr && cqRingNeedsEnter())` → `enter(fd, n, waitNr, flags, arg)`; else return `n` **with no syscall**. Return = count kernel consumed.
- **CQ overflow:** at the floor `NODROP` is present — kernel backlogs overflow and sets `CQ_OVERFLOW`; a `enter(GETEVENTS)` (even `to_submit=0`) flushes it. Size CQ ≥2×SQ + backpressure so this never happens in steady state.

---

## 6. FFM binding specifics (verified on JDK 25.0.3)

### 6.1 Syscall handles — one fixed-arity handle per syscall over variadic `syscall`

```java
private static final Linker LINKER = Linker.nativeLinker();
private static final MemorySegment SYSCALL = LINKER.defaultLookup().findOrThrow("syscall");
static final long SYS_io_uring_setup = 425, SYS_io_uring_enter = 426, SYS_io_uring_register = 427;

static final MethodHandle IO_URING_ENTER = LINKER.downcallHandle(SYSCALL,
    FunctionDescriptor.of(JAVA_LONG,          // return
        JAVA_LONG,                            // syscall number (arg 0, non-variadic)
        JAVA_INT, JAVA_INT, JAVA_INT, JAVA_INT, ADDRESS, JAVA_LONG),  // fd,to_submit,min_complete,flags,arg,argsz
    Linker.Option.firstVariadicArg(1),        // required even on Linux (ABI setup, e.g. %al on x86-64)
    Linker.Option.captureCallState("errno"));
// call: long n = (long) IO_URING_ENTER.invokeExact(errnoSeg, SYS_io_uring_enter, fd, toSubmit, minC, flags, MemorySegment.NULL, 0L);
```
`invokeExact` only (never `invoke` → boxing). Bind `mmap`/`munmap`/`eventfd`/`getrlimit` similarly (all in `defaultLookup()`; `mmap` is fixed-arity, not variadic).

### 6.2 errno capture (control syscalls + mmap only; **off the data hot path** — CQE `res` carries `-errno`)

```java
static final StructLayout CAPTURE = Linker.Option.captureStateLayout();
static final VarHandle ERRNO_VH = CAPTURE.varHandle(PathElement.groupElement("errno")); // coords (MemorySegment,long)
MemorySegment errnoSeg = ringArena.allocate(CAPTURE);            // ONE per ring, reused
// after a <0 return: int errno = (int) ERRNO_VH.get(errnoSeg, 0L);
```

### 6.3 mmap the ring; munmap as the reinterpret cleanup

```java
static final int PROT_READ=0x1, PROT_WRITE=0x2, MAP_SHARED=0x01, MAP_POPULATE=0x8000;
MemorySegment r = (MemorySegment) MMAP.invokeExact(errnoSeg, MemorySegment.NULL, len,
        PROT_READ|PROT_WRITE, MAP_SHARED|MAP_POPULATE, ringFd, offset);   // MAP_FAILED == address -1
return r.reinterpret(len, ringArena, seg -> munmap(seg, len));            // restricted; ties validity to arena
```
`MAP_POPULATE` prefaults so the first submit doesn't fault. Round `len` to the **runtime** page size.

### 6.4 Ring barriers — `getAcquire`/`setRelease` are the exact `smp_*` equivalents *(verified)*

```java
static final VarHandle U32 = JAVA_INT.varHandle();       // coords (MemorySegment, long offset); one handle for all counters
// submit: fill SQE (plain) -> write sq_array (plain) -> publish tail:
U32.setRelease(sqRing, sqTailOff, tail + n);             // == io_uring_smp_store_release(&sq.tail, ...)
// reap:
int cqTail = (int) U32.getAcquire(cqRing, cqTailOff);    // == smp_load_acquire(&cq.tail)
/* ...consume cqes[head & mask]... */
U32.setRelease(cqRing, cqHeadOff, cqHead);               // == smp_store_release(&cq.head, ...)
```
Use a standalone `JAVA_INT.varHandle()` (offsets come at runtime from `io_sqring_offsets`, not compile-time struct positions). Acquire/release generate exactly the barriers the shared-memory protocol needs without the full `volatile` StoreLoad fence — matters on aarch64. `VarHandle.acquireFence()` before the SQPOLL `kflags` read.

### 6.5 Arenas & buffer lifetime

- **Ring memory:** `Arena.ofConfined()` created **on the poller thread** if submit+reap are confined to it (fastest checks); `Arena.ofShared()` if foreign threads ever touch the ring segments directly. Close **only after** the in-flight registry drains (H1/H10).
- **I/O buffers:** *not* from a per-op arena. Take direct `ByteBuffer`s from the SPI `BufferAllocator` (Cassandra's `BufferPool`) and get the address via `MemoryUtil.getAddress(bb)` (`MemoryUtil.java:79`, Unsafe-based, no extra FFM) — thread-agnostic, GC-stable, lifetime tracks reachability across the submit→complete→consume thread hop.
- **Never hand the kernel a heap segment** — its address isn't GC-stable and async DMA would corrupt the heap. Assert buffers are direct.
- **Reachability:** the `PendingOp` in the in-flight map is the keep-alive; add `Reference.reachabilityFence(buffer)` at hand-off as belt-and-braces against the JIT treating the local as dead.

### 6.6 Performance

- Downcall overhead is tens of ns; **batching many SQEs per `io_uring_enter` amortizes it to ~zero** — don't micro-optimize the single call before batching.
- `Linker.Option.critical(boolean allowHeapAccess)` (the renamed `isTrivial`) skips the thread-state transition but **must not wrap a blocking call** — a blocking `io_uring_enter(min_complete>0)` under `critical` stalls GC for all threads. **Recommend a normal downcall** for `enter`; reserve `critical(false)` for genuinely trivial helpers. `allowHeapAccess=true` is irrelevant/dangerous here (DMA is async after the call returns).
- Preallocate `io_uring_params`, the errno segment, and scratch once per ring; SQE/CQE access is into the persistent mmap — no per-op allocation.

### 6.7 `--enable-native-access` & the JDK-25-only compile constraint

- On 25.0.3 the default is `warn` (downcalls run; one-time warning; module then marked native-access-enabled). The flag **silences the warning and is forward-compatible** (deny becomes default in a future release). `--illegal-native-access=deny` turns a missing flag into a fatal `IllegalCallerException` — use it in one CI cell to prove the wiring never rots.
- **Compilation** of `java.lang.foreign` code is fine only on **JDK 25** (preview on 21, absent on 17). Because CI compiles on 11/17/21/25 and there's no `--release`, the FFM sources are **compile-excluded on every JDK but 25**, and core code reaches them only through a **ServiceLoader SPI** — so no `Linker`/`MemorySegment` symbol is referenced on the <25 cells. Keep the static `Linker`/lookup init out of any class loaded on the <25 path.
- Upcalls (`Linker.upcallStub`) are **not needed** — completions are polled from CQ memory, no kernel→Java callback.

---

## 7. Public API sketch

Idiomatic Java mirroring liburing completeness. **Checkstyle note:** `java.util.concurrent.CompletableFuture` and `Semaphore` are banned in-tree; either annotate `// checkstyle: permit this import` (keeps the API JDK-standard and extractable — recommended for the library) or expose a library-owned `IoFuture`. Shown with `CompletableFuture`.

```java
// ---- Facade ----
public final class IoUring implements AutoCloseable {
    public static IoUring open(IoUringConfig cfg);        // setup + mmap; reads params.features
    public static Capabilities probeCapabilities();       // static, cheap, cached
    public int features();  public boolean supports(int opcode);   // via REGISTER_PROBE
    public SubmissionQueue sq();  public CompletionQueue cq();      // low-level escape hatch
    @Override public void close();                        // drain -> munmap -> close fd

    // async (idiomatic)
    public CompletableFuture<Integer> read (int fd, long off, ByteBuffer dst);
    public CompletableFuture<Integer> readv(int fd, long off, ByteBuffer[] dsts);
    public CompletableFuture<Integer> readFixed(int fixedIdx, long off, ByteBuffer dst, int bufIndex);
    public CompletableFuture<Integer> write(int fd, long off, ByteBuffer src);
    public CompletableFuture<Void>    fsync(int fd, boolean dataSync);
    public Batch batch();                                 // amortize one io_uring_enter over N ops
}
public interface Batch {
    Batch read(int fd, long off, ByteBuffer dst, CompletableFuture<Integer> out);
    int submit();  int submitAndWait(int minComplete);
}

// ---- Low-level ring (mirrors liburing get_sqe / submit / reap) ----
public final class SubmissionQueue { public Sqe getSqe(); public int spaceLeft(); public int ready(); }
public final class Sqe {                                  // op-prep; base + full surface
    public Sqe prepRw(int op, int fd, long addr, int len, long off);   // zeroes 64B first
    public Sqe prepRead(int fd, long addr, int len, long off);
    public Sqe prepReadFixed(int fd, long addr, int len, long off, int bufIndex);
    public Sqe prepReadv(int fd, long iovecsAddr, int nr, long off);
    public Sqe prepFsync(int fd, int fsyncFlags);
    public Sqe setData64(long userData);  public Sqe setFlags(int iosqeFlags);
    // full net/fs/ctl prep surface via op.Prep* helpers
}
public final class CompletionQueue {
    public long peekCqe();                                // 0 == none
    public int  forEachCqe(CqeConsumer c, int max);       // batch reap
    public int  ready();  public void advance(int n);     // setRelease khead
}
@FunctionalInterface public interface CqeConsumer { void accept(long userData, int res, int flags); }

// ---- Registration ----
public final class Registrar {
    public void registerBuffers(ByteBuffer[] slabs);      // getrlimit precheck; ENOMEM -> caller falls back
    public void registerFiles(int[] fds);  public void registerFilesSparse(int nr);
    public void registerFilesUpdate(int offset, int[] fds);
    public void unregisterBuffers();  public void unregisterFiles();
    public void registerRingFd();                         // ENTER_REGISTERED_RING
    public BufRing setupBufRing(int entries, int bgid, int flags);   // provided buffers (nice-to-have)
}

// ---- Event loop (thread-per-ring; async facade internals) ----
public final class IoUringEventLoop implements AutoCloseable {
    public CompletableFuture<Integer> submitRead(int fd, long off, ByteBuffer dst); // MPSC hand-off if foreign thread
    public void wakeup();                                 // eventfd, not NOP
    @Override public void close();                        // drain-before-close
}
```

`user_data` is the **only** correlation channel: a monotonic `long` token maps to a `PendingOp` (liburing's "stuff a pointer in user_data" has no FFM analogue). Sync convenience for Strategy C lives in the adapter: `int n = loop.submitRead(fd,off,buf).get();` — textually synchronous, unmounts the vthread carrier.

## 8. API completeness (liburing → Java), abbreviated

Legend **[MH]** must-have v1 · **[NH]** nice-to-have · **[X]** omit-for-v1. Full ~120-symbol matrix omitted; the priorities:

- **Setup/teardown [MH]:** `init(entries, params)`, `close()`, `features()`, `probe()/supports(op)`, `ring_dontfork` [NH].
- **SQE + prep [MH]:** `getSqe`, `prepRw` (base — zeroes 64 B), `prepRead/Readv/ReadFixed/Write/Fsync`, `sqeSetData64`, `sqeSetFlags`. Writev/ReadvFixed/WriteFixed/Fallocate/Ftruncate/Fadvise/Openat/Close/Renameat/Unlinkat/Cancel/LinkTimeout/FilesUpdate/MsgRing [NH]. All socket/poll/statx/splice/provide-buffers [X-for-Cassandra, present-in-library].
- **Submit/complete [MH]:** `submit`, `submitAndWait(n)`, `submitAndWaitTimeout(n,ts)`, `waitCqe`, `peekCqe`, `peekBatch`, `cqAdvance`/`cqeSeen`, `cqeGetData64`, `cqeRes`, plus the exact `flushSq`/`sqRingNeedsEnter`/`cqRingNeedsFlush` internals (§5.5). `sqSpaceLeft`/`cqReady` [MH]; `sqReady` [NH]; `sqringWait` [X, SQPOLL only].
- **Registration:** `registerFiles[Sparse/Update]`/`unregisterFiles` [MH]; `registerBuffers`/`unregisterBuffers` [MH, Phase 3]; `registerRingFd`/`registerEventfd`/`setupBufRing`/`registerSyncCancel` [NH]; personalities/restrictions/enable-rings [X].
- **Data/flag helpers [MH]:** `sqeSetData64`, `cqeGetData64`, `sqeSetFlags`; buffer-id decode + `cqeHasMore` [NH].

**Minimum viable general-purpose API (v1):** ~6 prep methods + ~10 submit/complete methods + file registration + the monotonic-token correlation infra — versus ~120 liburing symbols.

---

## 9. Memory-safety & correctness invariants (hazards → mitigations)

Rust bindings get these from the type system (`!Send`, ownership); this library must **enforce them at runtime.** The recurring theme: **the kernel can write into your memory after your Java code thinks the op is done.**

| # | Hazard | Failure mode | Mitigation |
|---|---|---|---|
| **H1 (critical)** | Arena.close()/GC frees memory while a kernel write is in flight | heap corruption / torn reads / cross-request leak / crash | In-flight registry keyed by `user_data` **owns the buffer**; never release to pool or close the Arena until that CQE is reaped. One long-lived Arena for ring+buffers, never a per-op Arena. |
| **H2** | heap `ByteBuffer` address instability | GC relocates; kernel DMAs to stale address | Only ever hand the kernel **direct/native** segments; assert it; `reachabilityFence` across the enter. |
| **H3** | SQE mutated/reused before kernel consumes it | wrong op/offset/len | Fill SQE fully → publish tail with **release**; reuse a slot only after `sq_head` (acquire) passed it. |
| **H4** | CQ overflow drops completions | future never completes → query hangs | `NODROP` present at floor; size CQ ≥2×SQ; backpressure semaphore; check `CQ_OVERFLOW` each reap and force a flushing `enter(GETEVENTS)`. |
| **H5** | `user_data` collision/recycling | late/dup CQE routes to wrong reader | **monotonic 64-bit** counter, never recycled; don't retire on `IORING_CQE_F_MORE` (multishot). |
| **H6** | SQ single-producer; multi-thread submit | corrupted submissions | **one ring per submitting thread**; foreign threads enqueue on an **MPSC** the owner drains. |
| **H7** | fd lifetime vs in-flight ops / fd reuse | op `-ECANCELED`, or reused fd hits wrong file | defer `close()` until in-flight count for that fd is 0, **or** use `REGISTER_FILES`+`IOSQE_FIXED_FILE` (Phase 3). |
| **H8** | cancellation race (ASYNC_CANCEL vs completion) | cancel "succeeds" but kernel still writes buffer | cancel is best-effort/async; **keep the buffer pinned until the original op's terminal CQE** (`-ECANCELED` or a real result); free only then. |
| **H9** | short read / partial completion (`res<len`) | stale tail → CRC/deserialization corruption | `res==0`⇒EOF; `0<res<len`⇒resubmit remainder at `off+res`; trim buffer limit to `res`. |
| **H10** | teardown/close with in-flight ops | munmap while kernel writes → H1 at shutdown | stop new reads → drain (`enter(GETEVENTS)` until registry empty, bounded timeout then `ASYNC_CANCEL` + await terminal CQEs) → close fd → **munmap** → **Arena.close()** last. |
| **H11** | `enter` EINTR/EAGAIN/EBUSY | spurious fatal / busy-loop | EINTR/EAGAIN retry; EBUSY reap-first then retry; `-ETIME` from a timeout wait is **success**. |
| **H12** | O_DIRECT triple alignment (addr/off/len) | `-EINVAL` in CQE (not at submit) | O_DIRECT is optional per path in v1. When enabled: align all three to the device logical block size via `FileUtils.getBlockSize()` + Agrona `allocateDirectAligned`/`align`; assert the buffer address is block-aligned before submit (the 2 KiB `BufferPool` unit is 512- but not always 4096-aligned — for chunk-sized ≥4 KiB power-of-two reads the pool is already ≥4096-aligned, but sub-page/unaligned reads need the aligned allocator or a 4096-unit pool). The buffered path (default for standard reads) has no alignment constraint. |
| **H13** | linked-op `-ECANCELED` propagation | misread as I/O error | surface the *head's* errno; don't set `IOSQE_IO_LINK` at all in v1. |
| **H14** | registered-buffer index reuse / RLIMIT_MEMLOCK (Phase 3) | wrong slab / ENOMEM | stable index→segment map; `getrlimit` precheck; fall back to unregistered `READ` on ENOMEM. |
| **H15** | FFM downcall pins a virtual thread | carrier not unmounted | keep `enter` non-blocking at submit; do the *waiting* on plain `future.get()` off the downcall (unmounts on 25 post-JEP-491). |

## 10. Concurrency & threading model

- **Thread-per-ring, not shard-per-core.** A bounded pool of N submit/poll threads (N ≈ `concurrent_reads` or CPU count), each owning **one ring** (single-producer SQ, single-consumer CQ). Cassandra isn't share-nothing; Seastar's shard-per-core is the Strategy-A-scale rewrite we defer.
- **Virtual threads as the suspension vehicle only** (Strategy C): the `ReadStage` task runs on a vthread, calls `future.get()`, and its carrier unmounts; the ring owner does submit+reap. **Do not** create a ring per vthread (FastThreadLocal/`BufferPool.LocalPool` thrash) — keep rings ∝ carriers and buffer allocation on the bounded carrier layer.
- **Foreign submitters hand off via MPSC** the ring owner drains (never submit to a ring you don't own). **Wakeup** a blocked poller via an **eventfd registered as an SQE** (JDK-Selector-style), not `IORING_OP_NOP` (which reintroduces a completion race).
- **Backpressure:** per-ring `Semaphore(queue_depth)` acquired before submit / released on CQE, CQ ≥2×SQ → CQ-overflow impossible in steady state.
- **SQPOLL off by default** (burns a core; needs `CAP_SYS_NICE` on 5.11, none on 5.13+); opt-in for dedicated I/O-saturated nodes.
- Loom gives *waiting*, not *batching* — the win depends on the **MPSC-into-shared-ring** path batching many reads per `enter`. This should be measured by the baseline microbench (§15), not assumed.

## 11. Availability probe & fallback (hardened — this is the *primary* path for many deployments)

io_uring is the most-restricted-in-practice Linux subsystem: Docker's default seccomp returns `EPERM`; `kernel.io_uring_disabled` sysctl (≥6.6; values 0/1/2) blocks it on hardened images; gVisor allows `setup` but fails ops; ChromeOS/Android disable it entirely; systemd `SystemCallFilter` may **`SCMP_ACT_KILL` (SIGSYS) the JVM** rather than return an error. (Google reported ~60% of its 2022 kCTF kernel exploits used io_uring.)

`checkIoUringAvailability` (a `StartupCheck`, modeled on `checkDirectIOSupport`):
1. **Gate on config** — only probe if a resolved mode is `io_uring` (operator opt-in; never probe unrequested).
2. **Cheap file check first** — read `/proc/sys/kernel/io_uring_disabled`; if present and `!= 0`, go straight to fallback (avoids touching a possibly-KILL-filtered syscall).
3. **OS/kernel gate** — `FBUtilities.isLinux`; kernel ≥ 5.6 via `Range.atLeast(new Semver("5.6", LOOSE))` (Guava `Range` + vdurmont `Semver`, `FBUtilities.getKernelVersion()`).
4. **Safe setup probe** — `io_uring_setup(1,&params)`; `ENOSYS`/`EPERM`/`EACCES` ⇒ unavailable→fallback. Record `params.features`; assert `NODROP`.
5. **Exercise a real op** — submit a `NOP`/`READ` on a scratch fd and reap its CQE (gVisor passes setup, fails ops); verify golden bytes for a READ.
6. **SIGSYS caveat** — optionally run the first raw probe in a short-lived forked helper so a KILL policy kills the probe, not the daemon. Cache the `Capabilities` once; never re-probe.
7. **Fallback** — if `io_uring_fallback_on_unavailable` (default true): `logger.warn(...)`, `setDiskAccessMode(standard)` **and `setIndexAccessMode(...)`** (both fields — the draft omits index). Else throw `StartupException` (`ERR_WRONG_MACHINE_STATE`/`ERR_WRONG_DISK_STATE`/`ERR_WRONG_CONFIG`). Mirrors `NativeTransportService.useEpoll()` → NIO.

---

## 12. Cassandra integration: `disk_access_mode=io_uring`

### 12.1 The enumerated seams (each verified file:line)

1. **Enum** — add `io_uring` to `Config.DiskAccessMode` (`Config.java:1355-1367`; honor the `checkKernelBug1057843` Javadoc contract).
2. **Main resolution** — `DatabaseDescriptor.java:674-693`: add a branch (in the trailing `else`, `:689-692`) setting `conf.disk_access_mode` + `indexAccessMode`. Decide io_uring's index-mode default (recommend: data=`io_uring`, index=`io_uring` for uncompressed, or keep index on `mmap` — measure).
3. **Compaction-read resolution** — `DatabaseDescriptor.java:695-708`: add an `else if (io_uring)` branch or the existing `IllegalArgumentException` (`:705`) fires.
4. **`FileHandle.ioMode()`** `:450-469` — add `case io_uring:` returning a new `ChannelProxy.IOMode.IO_URING` constant (`ChannelProxy.java:47-51`) or the `default:` `AssertionError` (`:467`) crashes.
5. **`FileHandle.complete()`** `:472-536` — add reader selection building `IoUringChunkReader` (compressed + uncompressed variants), plumbed through `maybeCached()` (`:538-543`).
6. **`ChannelProxy`** — new `IOMode` + `openOptions()` entry (`:69-80`); expose raw fd via `getFileDescriptor()` (`:218`, already exists) — or an `IoUringChannelProxy` subtype.
7. **`ChunkReader` impl(s)** — new class(es) implementing `readChunk(pos,buf)` (`ChunkReader.java:39`, contract is **thread-safe**), analogous to `SimpleChunkReader` (`:37-43`) / `CompressedChunkReader.{Standard,Direct}`.
8. **`ChunkCache`** — *optionally* `LoadingCache`→`AsyncLoadingCache` for in-flight dedup (`ChunkCache.java:57,150-156,160-174,232-252`; today synchronous + `ImmediateExecutor` at `:152`). Under Strategy C the sync cache + Loom already works; measure before switching. If switched, give Caffeine a **dedicated executor** so eviction `onRemoval→buffer.release()` doesn't run on the poller thread.
9. **Per-component builders** — no change if io_uring flows via `ioOptions.defaultDiskAccessMode`/`indexDiskAccessMode` (`SortedTableReaderLoadingBuilder:65`, `IndexComponent:34`, `BtiTableReaderLoadingBuilder:209/224`); the per-scan override `SSTableReader.canReuseDfile:1469-1474` needs an io_uring analogue to the direct-fallback clause.
10. **StartupCheck** — `checkIoUringAvailability` added to `DEFAULT_TESTS` (`StartupChecks.java:123-141`; `StartupCheck` iface `:35`), per §11.
11. **Fallback** — reset **both** `setDiskAccessMode(standard)` (`:4059`) and `setIndexAccessMode(...)` (`:4071`).
12. **Kernel-bug check** — because O_DIRECT reads are in scope from v1 (optional), extend `checkKernelBug1057843` (`:244-302`; today `getDirectIOWritePaths()` = write paths only) with a **read-paths hookup** so io_uring+O_DIRECT reads are covered by the ext4 6.1.64-6.1.66 guard. The buffered io_uring path is unaffected and needs no guard.
13. **cassandra.yaml docs** — extend the `disk_access_mode` block (`:444-459`) and `compaction_read_disk_access_mode` block (`:691-705`); add new tuning scalars (below).
14. **JVM/test options** — `--enable-native-access` already in `jvm25-server.options:115`; add to the ant/test/JMH harness (§14).

### 12.2 New `Config` scalars (not enum values)

`io_uring_queue_depth` (default 256), `io_uring_poller_threads` (default 1 per ring, pool sized to CPU/`concurrent_reads`), `io_uring_sqpoll` (default false), `io_uring_direct_io` (**per-path O_DIRECT toggle**; default off for the standard read path, on for compaction reads where the win is clearer — mirrors how `compaction_read_disk_access_mode` is separate from `disk_access_mode`), `io_uring_registered_buffers` (default false; Phase 3), `io_uring_registered_files` (default false; Phase 3), `io_uring_fallback_on_unavailable` (default true). Validate `io_uring_queue_depth >= concurrent_reads` (warn otherwise). When `io_uring_direct_io` is on, the reader opens the channel O_DIRECT and enforces H12 alignment; when off, it uses buffered `IORING_OP_READ`. Surface near the `compaction_read_disk_access_mode` block in `cassandra.yaml`.

### 12.3 Read-path async strategy (reconciling the earlier draft's A/B/C)

- **Strategy C — virtual-thread mounting (primary).** The read code stays textually synchronous; at the one io_uring seam it does `future.get()` on the completion. A vthread blocked on a `CompletableFuture` unmounts its carrier, giving M:N I/O concurrency **without** rewriting the deserialization stack. Cost: one seam, not ~40 files. Caveats designed around: no `synchronized` around a blocking wait in the read path; no blocking *inside* an FFM frame (H15); keep buffer alloc off ephemeral vthreads.
- **Strategy B — prefetch/readahead (scan optimization).** Fan out submissions across the SSTables a partition read touches and across scan chunks, consume synchronously. Useful for compaction/scans; leaves `DataInputPlus` untouched.
- **Strategy A — full continuation-passing (deferred, pre-designed).** Thread `CompletableFuture<BufferHolder> rebufferAsync(pos)` up through `RandomAccessReader` → iterators → `queryStorage` → `executeLocally` (~30-40 files). This is the version that historically never merged. It is **not started now**; a concrete execution plan is written in **Appendix C** for a future session to pick up if P2/P3 data proves virtual-thread suspension overhead is the residual bottleneck (Phase P4).

**Recommendation:** C primary, B for scans, A in reserve.

---

## 13. Extraction plan (mirror the `accord` submodule)

**Phase 0 (now): keep source in-tree, extraction-ready.** Source under `src/java/.../io/uring`, compiled only on JDK 25 (§14). **All** Cassandra coupling flows through the (h) SPI package + ServiceLoader. No `Config`/`DatabaseDescriptor`/`BufferPool`/`System.getProperty`/`java.io.File` reference anywhere inside `io.uring.*`. This alone makes later extraction a mechanical move.

**Phase X (later): lift to `modules/io-uring`.**
1. `git submodule add <repo> modules/io-uring`; add to `.gitmodules`; set `iouring.dir=modules/io-uring` in `build.xml` (mirror `accord.dir` at `:115`).
2. New `.build/build-io-uring.xml` modeled on `.build/build-accord.xml` (`gradlew clean build publishToMavenLocal`), but its Gradle **pins `source/targetCompatibility=25`** (accord uses 11) and the **module build is skipped when the outer JDK<25** (analogous to `-Dno-build-accord`). Wire into `_build_subprojects` (`build.xml:774`).
3. `.build/rat-include-io-uring.sh` (parent `git ls-tree HEAD` only sees the submodule pointer); disable the module's own rat/checkstyle in its Gradle (Cassandra re-runs them).
4. Layout `src/main/java/org/apache/cassandra/io/uring/{linux,abi,ring,op,async,reg,probe,spi}` — or rename the root to a neutral namespace (e.g. `dev.uring`) if published as a general artifact. Decide at extraction time (Open Q).
5. Minimal deps: JDK 25 (FFM) + slf4j + Agrona (standalone `BufferAllocator` default) only. **No Cassandra, no Netty, no liburing, no JNI `.so`.**
6. Every `.java` carries the ASF header; `META-INF/services` gets a rat `<exclude>`; Apache-2.0 license at module root.
7. Cassandra consumes the published `io-uring-*.jar` via the resolver; adapter classes + SPI impls stay in Cassandra's tree.

## 14. Build / test / packaging

**Compile model** — `build.xml:778-793`: `<javac source="${ant.java.version}" target="${ant.java.version}">`, **no `--release`** (comment `:779-780`); annotation processing `-proc:full` (`:538-545`, on for 21+); compile-time `--add-exports` for `sun.nio.ch`/`jdk.internal.ref` (`:532`) already present. **The FFM package compiles only on JDK 25.**

**Exact edits:**
1. **`build.xml:383`** — add `<string>--enable-native-access=ALL-UNNAMED</string>` to `_jvm25_arg_items`. Since `java-jvmargs` feeds the unit tests (`:1496`), `check-test-names` (`:1383`), and the JMH runner (`.build/build-bench.xml:101-102`), this one edit covers all three. (Server runtime already has it at `jvm25-server.options:115`.)
2. **`build.xml` ~545** — add a `ffm.src.excludes` condition (`""` on JDK25, else `org/apache/cassandra/io/uring/**,**/microbench/*IoUring*.java,**/io/uring/*Test.java`); apply `excludes="${ffm.src.excludes}"` to `_build_java` javac (`:782`) and `_build-test` javac (`:1321`). On 11/17/21 the FFM sources vanish; on 25 they build.
3. **New `src/resources/META-INF/services/<AsyncReadProvider>`** + a matching `<exclude>` in `.build/build-rat.xml` (a ServiceLoader file can't carry a comment header).
4. **Every new `.java`** — exact ASF header (rat audits git-tracked files, no `.java` exclusion); package `org.apache.cassandra.io.uring` (src) / `org.apache.cassandra.test.microbench` (benches).

**Quality gates** — the only static gates are **checkstyle + rat** (`ant check`; `eclipse-warnings` is a no-op stub; no forbidden-apis/import-control). Checkstyle rules that touch this package (`.build/checkstyle.xml`): **no ban on `java.lang.foreign.*` or `sun.nio.ch.*`** (clean); but **banned**: `java.util.concurrent.{CompletableFuture,Semaphore,CountDownLatch,Executors}` (`IllegalImport:104`), raw `System.getProperty` (`blockSystemPropertyUsage:179`), raw executors (`blockExecutors:88`), the **`var` keyword** (`:186`), `toLowerCase/UpperCase` (`blockToCases`), star imports; enforced `ImportOrder`/`UnusedImports`/`@Deprecated since=`. Use `// checkstyle: permit this import` for `CompletableFuture`/`Semaphore` in the library (recommended, keeps it extractable) or route through `org.apache.cassandra.utils.concurrent.*`. Run directly: `ant rat-check checkstyle checkstyle-test` (note: `.build/sh/ai-build` silently skips checkstyle).

**CI matrix** — GitHub Actions + CircleCI run `ant check` on **JDK 11 & 17**; Jenkins matrix builds/tests on **11,17,21,25**. Because the package won't compile on 11/17/21, it must be compile-excluded on all but 25, and **core must reach it only via ServiceLoader** (mandatory). Tests/benches don't exist on <25 cells (green by absence); on 25 cells they self-skip via the three `assumeTrue`s (below). Add one Jenkins/local cell running io_uring tests with **`--illegal-native-access=deny`** to prove the flag stays wired. Keep io_uring out of the simulator.

**Test/bench recipes:**
```
JAVA_HOME=/usr/lib/jvm/java-25-openjdk ant realclean build build-test    # FFM compiles only here
ant rat-check checkstyle checkstyle-test                                  # static gates (run directly)
ant testsome -Dtest.name=org.apache.cassandra.io.uring.IoUringBindingTest # unit
ant microbench -Dbenchmark.name=IoUringChunkReader -Djmh.args="-f 1 -wi 3 -i 5 -t 8"   # JMH (bench class must end in "Bench", pkg must contain "microbench")
```
Runtime self-skip in every FFM test:
```java
assumeTrue(FBUtilities.isLinux);                        // FBUtilities.java:128
assumeTrue(Runtime.version().feature() >= 25);
assumeTrue(IoUringAvailability.check().isAvailable());  // kernel≥5.6 + setup probe; ENOSYS/EPERM -> skip
```

---

## 15. Test & validation plan

Unit/property/adapter tests under `test/unit/org/apache/cassandra/io/uring/…`; benches under `test/microbench/org/apache/cassandra/test/microbench/`. The **`FakeIoUringRing`** — a programmable in-Java ring returning specified partial fills, `-errno`, and `CQ_OVERFLOW` — is the single most important test asset: it keeps unit/property/fault coverage green on non-Linux/non-25 cells and in the deterministic simulator.

| Category | Test class(es) | What it asserts | Tooling |
|---|---|---|---|
| **Golden-oracle unit** | `IoUringGoldenReadTest` | byte-for-byte equality of ring `READ` vs `FileChannel.read`/`pread` across offsets/lengths/EOF/**>4 GiB**, on tmpfs (fast) and a real block device | JUnit + tmpfs |
| **ABI layout guard** | `IoUringLayoutTest` | `Layouts.*.byteSize()`/`byteOffset()` == ground truth (params=120, sqe=64, cqe=16, sq_off@40/cq_off@80); on **x86-64 AND aarch64** | JUnit + per-arch `Assume` |
| **Barrier / ring cycle** | `IoUringBindingTest`, `IoUringRingCycleTest` | setup→enter→exit round-trip; get_sqe-full→submit→reap; `cqReady`/`sqSpaceLeft` accounting; short-read resubmit | JUnit |
| **Property / fuzz** | `IoUringPropertyTest` | `qt()` random op sequences (mixed offset/len, interleaved submit/reap, forced short reads, injected cancels, ring-full); invariants: golden bytes + no lost/duplicated `user_data` | quicktheories + `FakeIoUringRing` |
| **Fault injection** | `IoUringCqeErrorTest`, `IoUringBackpressureTest`, `IoUringFallbackTest`, `IoUringMemlockTest` | negative `res`(`-EIO`)→`FSReadError`; `EINTR` retry; `EBUSY`/ring-full retry; `setrlimit(MEMLOCK,small)`→`ENOMEM`→unregistered fallback; `ENOSYS`→fallback to `standard` **and reset `indexAccessMode`**; CRC mismatch→`CorruptSSTableException` | `FakeIoUringRing` + Byteman `Injections` |
| **Leak detection** | `IoUringLeakTest` | after teardown: `InflightRegistry.size()==0`, `/proc/self/fd` back to baseline, Arena/segment alloc==free, RSS stable; **poisoned-buffer test** for write-after-free (H1) | JUnit + `/proc` polling |
| **Soak / stress** | `IoUringSoakTest` (test/long or test/burn) | sustained high QD, many rings, long duration; steady-state correctness + resources return to baseline | test/long |
| **JMH micro-bench** | `IoUringBatchedReadThroughputBench` (**the baseline bench** — still run, but informational, not a gate; throughput vs QD {1,4,8,16,32,64,128}), `IoUringChunkReaderBench` (latency p50/p99/p999, `Mode.SampleTime`), `CompactionReadModeBench`, `ChunkCacheMissLatencyBench` | io_uring vs **`standard` pread vs mmap vs O_DIRECT** at chunk {4,16,64,256 KiB}; registered vs unregistered; force misses. Compare honestly (JUring shows FileChannel winning at high writer concurrency); results steer default per-path config | `ant microbench`, `@Fork(jvmArgsAppend="--enable-native-access=ALL-UNNAMED")` |
| **Adapter parameterized** | add `io_uring` to `CompressedRandomAccessReaderTest`, `RandomAccessReaderTest`, `SSTableReaderDataReaderTest`; `DatabaseDescriptorTest.testIoUringDiskAccessMode` | reader parity across access modes; resolution correctness | `@RunWith(Parameterized.class)` |
| **dtest** | `IoUringRepairTest`, `IoUringCompactionReadTest`, `IoUringDiskFailurePolicyTest` | `.set("disk_access_mode","io_uring")`; no `FSReadError` in logs; row correctness post-compaction; `disk_failure_policy=stop` on injected error | in-JVM dtest |
| **Differential oracle** | `HarryIoUringReadTest` | identical `HistoryBuilder` under `io_uring` vs `standard` → identical `QuiescentChecker` model state; any divergence = bug | Harry |
| **Availability gating** | `IoUringAvailabilityTest` + a CI cell with `--illegal-native-access=deny` | sysctl-file path, setup-EPERM path, exercise-real-op path; deny-cell proves `--enable-native-access` stays wired | JUnit + CI |
| **Simulator negative** | sim-harness assertion | `io_uring` is **never** requested under simulation (stays `standard`) | simulator |

**Native-memory "sanitizer-equivalent":** FFM `MemorySegment` bounds-checks catch OOB Java-side; a small C reproduction of the raw ABI under ASan/valgrind cross-checks the layout; the poisoned-buffer leak test is the real defense against H1 (fill with a sentinel, aggressively free-then-realloc while a `FakeRing` writes late, assert no sentinel escapes).

---

## 16. Phased delivery & go/no-go gates

Every phase is independently revertible. The facade's memory-safety invariants (H1/H8/H10) are proven under soak **before** any Cassandra data flows through it (the earlier draft folds P1/P2 together; splitting them is deliberate).

| Phase | Deliverable | Files touched | Gate metric | Revert |
|---|---|---|---|---|
| **P0 — Binding proven in isolation** | Core layers (a)–(d),(g) + minimal (e)/(f); `FakeIoUringRing`; golden-oracle + ABI-layout + property tests; JMH benches. `--enable-native-access` in `build.xml:383`; `ffm.src.excludes`; ServiceLoader skeleton. | new `io.uring.*`; `build.xml`; rat `<exclude>` | **Correctness gate + baseline measurement (no go/no-go):** golden-oracle/ABI/property/leak tests green; **run** the io_uring vs `standard`/O_DIRECT/mmap benchmark at QD≥16 + single-read latency and record it. The perf numbers steer default per-path config (which paths enable io_uring/O_DIRECT) but do **not** block proceeding. | delete package; no core wiring exists yet |
| **P1 — async facade** | Full (e): thread-per-ring event loop, `InflightRegistry`, `CompletionDispatcher`, `Backpressure`, MPSC hand-off, eventfd wakeup, drain-before-close. SPI defaults. | `io.uring.async.*`, `io.uring.spi.*` | **GATE 1:** soak/leak clean (registry drains; fd/RSS baseline); fault matrix green; batched-submit throughput scales with QD in isolation | facade unused by Cassandra; drop async package |
| **P2 — ChunkReader/ChunkCache seam** | `IoUringChunkReader` + `IoUringChannelProxy` **[ADAPTER]**; `Config` enum + `DatabaseDescriptor` branches + `FileHandle.ioMode()`/`complete()` cases + `ChannelProxy.IOMode`; `checkIoUringAvailability` (+ ext4 read-path hookup); SPI adapters; yaml docs. Buffered reads by default; **`io_uring_direct_io` toggle** wired with H12 alignment; sync `LoadingCache` + Loom. | `Config.java:1355`, `DatabaseDescriptor.java:674-708`, `FileHandle.java:450-536`, `ChannelProxy.java:47-51`, `StartupChecks.java:123-141,244-302`, `cassandra.yaml` | **GATE 2:** correctness parity (Harry + parameterized + dtest green) for **both** buffered and O_DIRECT; read-heavy p99 ↓ on cache-cold NVMe; **no regression** on hot-cache | one enum value + branches; `disk_access_mode=standard` fully disables (reset both fields) |
| **P3 — registered buffers/files + integration** | (f) wired into the read path; **`compaction_read_disk_access_mode: io_uring` with O_DIRECT on** (clearest beneficiary); Strategy-B readahead for scans; registered buffers (RLIMIT_MEMLOCK-gated); `AsyncLoadingCache` if P2 data justifies it. | `io.uring.reg.*`, `SSTableReader.canReuseDfile:1469`, compaction scanners, `IoUringManager` | **GATE 3:** compaction/scan MB/s ↑ with O_DIRECT + registered buffers vs P2; RLIMIT_MEMLOCK fallback verified | `io_uring_registered_buffers=false` disables registration; mode revert as P2 |
| **P4 — (deferred, pre-designed) CPS hot path** | Strategy A async iterators per **Appendix C**, **only** if P2/P3 shows vthread-suspension overhead is the residual bottleneck (~30–40 files) | `Rebufferer`/`ChunkReader` + iterator stack | proven residual bottleneck | large; gated behind proven need — Appendix C plan exists but execution is a future session |
| **P5 — (deferred, pre-designed) cache-miss-adaptive read mode** | An `io_uring_read_mode=adaptive` per **Appendix D**: a buffered `RWF_NOWAIT` probe serves OS-page-cache hits at RAM speed and falls back to `IORING_OP_READ` on `O_DIRECT` (its 3.6× device-bound win) only on a real page-cache miss. Gated on measured benchmark findings (Appendix D). | `IoUringChunkReader` (two-fd), `Config` read-mode knob | adaptive beats **both** pure `standard` and pure O_DIRECT on a mixed warm/cold workload | one enum/knob; revert to `direct` or `standard` |

**Effort shape:** P0 is the bulk of the *library* engineering and the highest-value de-risking step; P2 is the bulk of the *integration* work; P3 is optimization; P4 is contingency.

### 16.1 P3 `READ_FIXED` gate result (measured; kept off by default)

The `READ_FIXED` registered-buffer path is built and kernel-verified end to end: `BufferRegistrar` gained a sparse-reserve (`REGISTER_BUFFERS2` with `RSRC_REGISTER_SPARSE`) + incremental `REGISTER_BUFFERS_UPDATE` pair; the `IoUring` facade fans both across every ring so a slot's `buf_index` is consistent regardless of which ring routes the read; and `FixedSlabRegistry` maps a chunk-cache buffer to the `buf_index` of the `BufferPool` macro-chunk (slab) that contains it, registering that slab on demand. The `IoUringAsyncTest.readFixedThroughSparseRegisteredSlab` case exercises sparse-reserve + non-zero-slot fill + sub-region `READ_FIXED` on the live kernel (8/8 green).

`IoUringBatchedReadThroughputBench` was extended with an `ioUringBatchedFixed` arm (same ring, depth, buffers, offsets; only `READ` → `READ_FIXED` differs) to isolate the registered-buffer delta:

| chunk | depth | `READ` ops/s | `READ_FIXED` ops/s |
|------:|------:|-------------:|-------------------:|
| 16 KiB | 32 | 13855 ± 5385 | 12733 ± 4757 |
| 16 KiB | 64 | 7120 ± 4341 | 6563 ± 3437 |
| 64 KiB | 32 | 7099 ± 2068 | 6180 ± 3252 |
| 64 KiB | 64 | 3183 ± 1451 | 3276 ± 1759 |

For page-cache-resident reads the two are statistically indistinguishable (error bars overlap in every config; point estimates favor plain `READ` in three of four). This matches the mechanism — `READ_FIXED` only saves per-I/O pinning of the destination pages, which is negligible when the copy dominates and the pages are already faulted. The `O_DIRECT`-DMA case (where pinning could matter) is where a win might appear, but this host's 8 MiB `RLIMIT_MEMLOCK` hard cap blocks the high-depth × 64 KiB O_DIRECT point, and the standalone O_DIRECT benchmark already attributed io_uring's device-bound advantage to async submission rather than to buffer registration. **Decision:** `READ_FIXED` stays off by default (gated behind `-Dcassandra.io_uring.read_fixed=true`); the standard chunk-cache path uses unregistered `IORING_OP_READ`. Re-evaluate only on hardware with a raised memlock limit and a device-bound (cold, O_DIRECT) mixed workload.


---

## 17. Risks & completeness critique (what a skeptic attacks)

- **The win may not exist, and honest data may be embarrassing.** io_uring only helps device-bound cache misses; hot reads never syscall. JUring's JMH shows `FileChannel` beating io_uring at ≥20 writers/64 KiB; ScyllaDB measured ~5% over tuned linux-aio. **No actual Cassandra-workload number exists yet** — that gap is the single biggest project risk. The P0 microbenchmark (§15) must compare against `standard`/O_DIRECT/mmap at realistic QD and be reported honestly; per the confirmed decision it now steers *where* io_uring is enabled by default (per-path config) rather than acting as a go/no-go gate on the project.
- **Memory safety is by convention, not the compiler.** A skeptic hunts the one path that frees/reuses a buffer or `munmap`s before the terminal CQE — under cancellation, teardown, or short-read resubmit. The `InflightRegistry` + drain-before-close + memory-safe-cancel + poisoned-buffer test are the defense; specify them precisely.
- **Virtual-thread mounting is asserted, not measured.** Carrier unmount cleanliness, FastThreadLocal thrash avoidance, and (critically) whether reads actually *batch* into the shared ring rather than trickling one-per-vthread — all unproven. Loom gives waiting, not batching. The MPSC-into-shared-ring batching path needs an explicit microbench.
- **Operational availability is worse than it looks.** Docker default seccomp → EPERM; hardened images `io_uring_disabled=2`; gVisor setup-ok/ops-fail; `SCMP_ACT_KILL` can SIGSYS the JVM at probe time. For a large fraction of deployments io_uring is unavailable, so the **fallback path is the primary path** and must be flawless (including resetting `indexAccessMode`).
- **CQ-overflow / lost-completion → query hang.** A dropped/misrouted CQE means a future never completes. Requires `NODROP` asserted, CQ≥2×SQ, monotonic never-recycled `user_data`, backpressure, and multishot-aware retirement. Probe the overflow-flush path under saturation.
- **Simulator blind spot.** Completions arrive off the deterministic scheduler, so Cassandra's strongest correctness tool can't cover this path. Assurance leans on Harry + fault injection + `FakeIoUringRing` — a real reduction for a critical-path reader.
- **ABI drift.** The compile host's header exposes opcodes/features absent on RHEL 9 / Ubuntu 22.04. Every optional path must be `REGISTER_PROBE`/`features`-gated; a skeptic finds the one place that trusts the compiled constant.
- **Under-specified corners:** exact O_DIRECT alignment story if/when enabled (2 KiB pool unit is 512- but not always 4096-aligned); `AsyncLoadingCache` vs sync+Loom for in-flight dedup (measure); concrete deferred-fd-close design (H7).

## 18. Decisions (confirmed) & remaining defaults

**Confirmed by the maintainer (2026-07-05):**
1. **Library scope — full surface + full async.** The library implements not just the storage prep set but the *complete* async ergonomics (sockets/accept/connect/send/recv, multishot, zero-copy, provided-buffer rings), so it stands alone as a general-purpose async-I/O library. Cassandra wires the storage-read subset; write preps are present for future commitlog/flush consumers.
2. **Binding — pure raw-`syscall` FFM only.** No JNI, no C shim, no `liburing`. The draft's "Option B / shim fallback" is dropped entirely.
3. **v1 device access — O_DIRECT from the start, optional/configurable per path** (`io_uring_direct_io`). Default off for the standard point-read path (unclear benefit), on for compaction reads (clear benefit); future commitlog/flush writes are also O_DIRECT beneficiaries. Buffered `IORING_OP_READ` is the other option. Alignment (H12) and the ext4 kernel-bug read-path hookup are therefore in scope from v1.
4. **Read-path async — Strategy C now; CPS pre-designed, deferred.** Implement virtual-thread mounting (C) for P2/P3. A concrete CPS (Strategy A) execution plan is written in **Appendix C** to be executed in a future session if warranted (P4).

**Remaining defaults (not separately raised; flag if you disagree):** kernel floor **5.6**; **SQPOLL** opt-in/off; **registered buffers/files** Phase-3 opt-in (RLIMIT_MEMLOCK-gated); `SINGLE_ISSUER|DEFER_TASKRUN` feature-gated on ≥6.1; **concurrency** thread-per-ring with MPSC foreign hand-off + vthread suspension (not shard-per-core, not ring-per-vthread); **extraction** in-tree behind the SPI now, lift to `modules/io-uring` after Gate 2; **namespace** decision (keep `org.apache.cassandra.io.uring` vs a neutral `dev.uring`) deferred to extraction time.

## Appendix A — verified ABI quick-reference

```
syscalls (x86-64 = aarch64):        setup=425 enter=426 register=427
params=120B  sqring_off=40B  cqring_off=40B  sqe=64B  cqe=16B  iovec=16B  timespec=16B
sqe:  opcode@0 flags@1 ioprio@2 fd@4 off@8 addr@16 len@24 rw_flags@28 user_data@32 buf_index@40
cqe:  user_data@0 res@8 flags@12                     (res<0 => -errno; res==0 => EOF)
op:   NOP=0 READV=1 FSYNC=3 READ_FIXED=4 WRITE_FIXED=5 READ=22 WRITE=23 READV_FIXED=60
mmap: SQ_RING=0 CQ_RING=0x8000000 SQES=0x10000000    PROT_READ|WRITE, MAP_SHARED|MAP_POPULATE
feat: SINGLE_MMAP=1<<0(5.4) NODROP=1<<1(5.5) EXT_ARG=1<<8(5.11)
setup:CQSIZE=1<<3 SQPOLL=1<<1 SINGLE_ISSUER=1<<12(6.0) DEFER_TASKRUN=1<<13(6.1) NO_SQARRAY=1<<16(6.6)
reg:  BUFFERS=0 FILES=2 PROBE=8 FILES2=13 BUFFERS2=15 RING_FDS=20 PBUF_RING=22 SYNC_CANCEL=24
barriers: SQ.tail=setRelease  SQ.head=getAcquire  CQ.tail=getAcquire  CQ.head=setRelease
```

## Appendix B — key file:line index (branch `cscotta/jdk25`)

`Config.DiskAccessMode` `config/Config.java:1355-1367` · `disk_access_mode` field `:130` · `concurrent_reads` `:251` · compaction/commitlog/bgwrite modes `:487/:486/:416`.
`DatabaseDescriptor` resolution `674-693` (direct rejected `685-687`), compaction-read `695-708`; `indexAccessMode` `:225`; getters/setters `4052/4059/4064/4071`; `hasLargeAddressSpace` `4794`.
`FileHandle` `ioMode()` `450-469` (AssertionError `:467`), `complete()` `472-536`, `maybeCached` `538-543`, `withDiskAccessMode` `:393`.
`ChannelProxy` `IOMode` `47-51`, `openOptions` `69-80`, `read` `169-180`, `map` `194-204`, `getFileDescriptor` `218-221`.
`ChunkReader` iface `:39` (thread-safe `:29`) · `SimpleChunkReader` `37-43,57-64` · `CompressedChunkReader.{Standard:399,Direct:296,Mmap:474}`.
`Rebufferer` `:36` · `ChunkCache` `load:160-174`, `wrap:187-190`, `LoadingCache:57`, `ImmediateExecutor:152`, `CachingRebufferer:232-252`, `Buffer` CAS `98-144`.
`StartupChecks` `DEFAULT_TESTS:123-141`, `checkDirectIOSupport:872-909`, `checkKernelBug1057843:244-302` (Semver import `:53`, Range `:52`); `StartupCheck` iface `service/StartupCheck.java:35`; `StartupException` codes `:26-29`.
`NativeLibrary.getfd` `379-391/398-414` · `BufferPool` sizes `131-135`, `MACRO_CHUNK_SIZE:389`, `allocateDirectAligned:1099`, `infiniteLoop:199`; `BufferPools.CHUNK_CACHE_POOL:39` · `MemoryUtil.getAddress:79`, `pageSize:74` · `FileUtils.getBlockSize:789`.
`ExecutorFactory.infiniteLoop:182-185`, `Global.executorFactory:200`; `InfiniteLoopExecutor.SimulatorSafe:55` · `SocketFactory` lifecycle `181-184,283-288` · `CassandraDaemon.setup:243`/`runStartupChecks:275` · `StorageService.addPreShutdownHook:4091` · `NativeTransportService.useEpoll:136-144`.
`BufferPoolMetrics` `metrics/BufferPoolMetrics.java:29,52` · `ChunkCacheMetrics:36,49-50` · `FBUtilities.isLinux:128`, `getKernelVersion:1461`.
Build: `javac` model `build.xml:778-793`, `-proc:full` `538-545`, `_jvm25_arg_items` `339-384` (flag → `:383`), `_build_java` `:782`, `_build-test` `:1321`, `microbench` `.build/build-bench.xml`, checkstyle `.build/checkstyle.xml`, rat `.build/build-rat.xml`, accord model `.build/build-accord.xml`+`:774`+`:115`; runtime flag already at `conf/jvm25-server.options:115`.

## Appendix C — deferred CPS (Strategy A) execution plan (for a future session)

**Trigger:** execute this only if P2/P3 measurements show virtual-thread suspension/scheduling overhead (not device latency) is the dominant residual cost on the cache-cold hot path — i.e. Strategy C has been shipped and proven insufficient. Do not start speculatively.

**Goal:** make rebuffering return a future so the read path can drive many in-flight device reads from a single carrier without a virtual thread per read, threading `CompletableFuture` (or `utils.concurrent.Future`) through the deserialization stack.

**Ordered work (~30–40 production files):**
1. **Introduce the async seam.** Add `Rebufferer.rebufferAsync(long pos) → Future<BufferHolder>` (`Rebufferer.java:36`) and `ChunkReader.readChunkAsync(...)` (`ChunkReader.java:39`), default-implemented in terms of the sync methods so every existing impl keeps working unchanged.
2. **Async cache.** Switch `ChunkCache` from `LoadingCache` to Caffeine `AsyncLoadingCache` (`ChunkCache.java:57,150-174`); `CachingRebufferer.rebufferAsync` composes on the in-flight future (turns the TOCTOU retry loop `232-252` into a `thenCompose` re-get on `reference()==null`); give Caffeine a dedicated executor so eviction `onRemoval→buffer.release()` never runs on the ring poller.
3. **Reader → iterator.** Thread the future through `RandomAccessReader.reBufferAt` (`:78-88`) — the hard part, since every `DataInputPlus` read may re-buffer — then `Walker.go` (`tries/Walker.java:94`), `BigTableReader.getRowIndexEntry`/`BtiTableReader.getExactPosition`, `AbstractSSTableIterator` (`:76`).
4. **Command level.** `SinglePartitionReadCommand.queryStorage` (`:556`) and `ReadCommand.executeLocally` (`:506`) return a future; `StorageProxy.LocalReadRunnable` (`:2740`) completes the `ReadCallback` on CQE instead of blocking `Stage.READ`.
5. **Prefetch coordinator (Strategy B graft).** At the command level, issue the parallel reads a query is known to need (multiple SSTables per partition; readahead chunks per scan) as a batch to one ring, so CPS + batching compound.

**Invariants to preserve:** the §9 memory-safety contract is unchanged (the async future still owns the buffer via the in-flight registry until the terminal CQE); hot-cache reads must complete synchronously (already-present future, no thread hop); the simulator path stays on `standard`. **Fallback:** keep the sync methods as the default so non-io_uring modes and `AsyncLoadingCache`-miss paths continue to work; CPS is an additive fast path, not a replacement.

**Risk:** this is the change that historically never merged to trunk; its blast radius touches the widest interfaces in the storage engine (`DataInputPlus`/`FileDataInput`). Land it behind a flag, one layer at a time, with the parameterized reader suites (§15) green at each step.

## Appendix D — Empirical read-throughput findings & a cache-miss-adaptive io_uring read mode (future phase P5)

**Provenance:** measured 2026-07-05 on the dev host (aarch64 / Apple Silicon, 10 cores, 62 GiB RAM, NVMe **btrfs**, kernel 7.0.13, OpenJDK 25.0.3) with the P0/P1 binding via a purpose-built JMH bench (`test/microbench/.../IoUringVsBufferedIoBench.java`; reports in `io_uring_bench/`). Numbers are **directional and host-specific** (one device, one kernel) — the *shape* generalizes; the absolute GB/s and cross-over points do not. Re-measure on RHEL 9 (5.14) / Ubuntu 22.04 (5.15) before trusting.

### D.1 What was measured (io_uring FFM binding vs `java.nio.channels.FileChannel`)

Two cache regimes × 3 workloads × chunk 4/8/16/32/64 KiB. **WARM** = buffered, file resident in page cache (no device I/O). **COLD** = `O_DIRECT` on both sides (page cache bypassed, `posix_fadvise(DONTNEED)` in setup; on btrfs, evicting requires a prior flush — dirty pages are not evictable). io_uring keeps `qd` ops in flight per thread and reaps one batch per `io_uring_enter`; `FileChannel` issues `qd` blocking positioned calls (one in flight per thread). Throughput in GB/s.

| Regime / setting | Result (io_uring ÷ FileChannel) |
|---|---|
| **WARM**, QD 32, 1 thread | FileChannel faster in **all** configs (io_uring 0.37–0.75×) — pure mechanism overhead, no device latency to overlap. |
| **WARM**, QD 128, 4 threads | io_uring **ahead** on small random (4 KiB **1.14×**); still behind on large (64 KiB **0.37×**; warm 64 KiB = FileChannel 46–52 GB/s vs io_uring 17–18, a page-cache `memcpy` is unbeatable). |
| **COLD / O_DIRECT**, QD 128, 4 threads | io_uring **wins reads**: random **2.4–6.4×** (4 KiB **6.39×**, 64 KiB **3.60×**), sequential **2.4–3.7×**. Writes ~**parity/slightly behind** (0.86–0.97×). 64 KiB cold: io_uring 6.5–7 GB/s vs FileChannel+O_DIRECT 1.8–1.9. |
| **COLD random 4 KiB QD scaling**, 1 thread | io_uring ÷ FileChannel = 1.01× (QD1) → 3.2× (QD4) → 6.5× (QD16) → 11.3× (QD64) → **16.0× (QD256)**; blocking FileChannel flatlines (one I/O in flight). |

### D.2 Conclusions that drive this phase

1. **io_uring's read win is confined to the device-bound regime, and `O_DIRECT` is the enabler.** Buffered io_uring never beats a warm page-cache read; O_DIRECT io_uring beats blocking O_DIRECT by ~3.6× at 64 KiB because one thread keeps the queue full while a blocking reader has one I/O outstanding.
2. **The decisive variable is cache residency, not chunk size.** At 64 KiB the warm and cold regimes point in *opposite* directions (buffered wins warm ~2.9×; io_uring wins cold ~3.6×).
3. **Queue-depth sizing is workload-shaped:** small random reads are IOPS-bound (deep queues pay off to QD≈256); large reads are bandwidth-bound (QD≈8–32 saturates the device by Little's law, deeper wastes pinned memory).
4. **Writes show no benefit here** (device write-cache absorbs them; O_DIRECT on btrfs is CoW) — keep the write path buffered/`standard`.

This is exactly the "io_uring only helps cache misses that reach the device" thesis from §0.1, now with numbers. It motivates a read mode that pays io_uring's cost **only** on true device reads.

### D.3 Can a cache miss be determined? (the crux — answered)

There are **two** caches in front of a data read, and both misses are determinable:

- **ChunkCache miss — structural, free.** Cassandra's `ChunkReader` is invoked *only* by the `ChunkCache` loader on a miss (§4 read call-chain 3a; `CachingRebufferer` calls the reader on miss, hot chunks are served from the off-heap cache with **no syscall**). So making io_uring the `ChunkReader` (§12) already means **io_uring runs only on ChunkCache misses** — the "fall back to io_uring on ChunkCache miss" behavior is the default structure, not extra work. Because the loaded chunk is then cached, a chunk is read from the device at most once between evictions regardless of read mode.
- **OS page-cache miss — determinable via `RWF_NOWAIT` (empirically verified on this host).** A buffered `IORING_OP_READ` with `sqe.rw_flags = RWF_NOWAIT (0x8)` completes inline with the data on a page-cache **hit** and returns **`-EAGAIN` (`res == -11`)** on a **miss** (verified 3/3: cold `res=-11`, hot `res=4096`; the probe is *the* read, so there is no TOCTOU window). `RWF_NOWAIT` (preadv2) exists since kernel 4.14 and io_uring honors it — well below the 5.6 floor. **This is the miss detector the "if that can even be determined" question was about: yes, it can.** (Caveat: io_uring will transparently punt a *non*-`RWF_NOWAIT` blocking read to an io-wq worker and return the data, which is *not* a miss signal — the explicit `RWF_NOWAIT` is what surfaces `-EAGAIN`.)

### D.4 The proposed mode(s) — `io_uring_read_mode ∈ { direct, buffered, adaptive }`

All three run only on ChunkCache misses (per D.3). The choice is what to do with the device/page-cache layer:

- **`direct` (simplest — the P3 default for compaction/scan).** Every ChunkCache-miss read is `IORING_OP_READ` on an `O_DIRECT` fd. Best when the workload is device-bound anyway (compaction, large scans) or the ChunkCache holds the hot set. Downside: bypasses the OS page cache entirely, so a chunk that *was* page-cache-warm but ChunkCache-cold pays full device latency.
- **`buffered`.** ChunkCache-miss reads via buffered `IORING_OP_READ`. Gets page-cache hits but pays io_uring overhead and (per D.1) loses to plain `FileChannel` on warm large reads — least attractive; mostly a correctness/fallback baseline.
- **`adaptive` (the hybrid — recommended candidate for the general point-read path).** Per read:
  1. Submit buffered `IORING_OP_READ` + `RWF_NOWAIT` on the **buffered fd**.
  2. `res ≥ len` → OS page-cache **hit** → return at RAM speed (no O_DIRECT penalty). Done.
  3. `res == -EAGAIN` → page-cache **miss** → resubmit as `IORING_OP_READ` on the **`O_DIRECT` fd** (aligned) → device-bound, io_uring's 3.6× win.
  4. `0 < res < len` → partial hit → serve the cached prefix, resubmit the remainder from `off+res` via the same logic (reuse the §9 H9 short-read machinery).

  This composes the three layers so each miss falls to the next-fastest mechanism and O_DIRECT is applied *exactly* where it wins (true device reads): **ChunkCache (hot, no I/O) → OS page cache (warm, buffered, RAM speed) → device (cold, `O_DIRECT` + queue depth)**.

### D.5 Engineering considerations & risks (for `adaptive`)

- **Two fds per data file.** `O_DIRECT` is a per-`open` flag, so keep both a buffered fd (probe / warm hits) and an `O_DIRECT` fd (cold fallback); register both as fixed files (§ (f) `FileRegistrar`). `fcntl(F_SETFL, O_DIRECT)` toggling is rejected — racy under concurrency. **Coherence is a non-issue for SSTable data files** because they are immutable after write, which is precisely what makes mixing a buffered and an O_DIRECT fd on one file safe here.
- **Alignment (H12).** The O_DIRECT fallback needs 512/4096-aligned offset+length+buffer. Chunk-aligned reads already satisfy this; sub-chunk / compressed-frame reads need the aligned allocator. Reuse the P2/P3 O_DIRECT alignment path.
- **Probe overhead.** The miss path costs one extra non-blocking `enter`/CQE before the O_DIRECT resubmit — negligible against device latency and amortized by batching probes per `enter`. The **hot path must not regress**: measure that `adaptive` on a 100%-page-cache-resident workload is within noise of `buffered`/`standard`.
- **Short reads / partial hits** are normal with `RWF_NOWAIT` (cached prefix then the EAGAIN boundary); the terminal-CQE / short-read-resubmit invariants (§9 H9) already cover this — the uncached remainder routes to O_DIRECT.
- **fd lifetime & drain (H7/H10)** apply to *both* fds.
- **Portability of the detector.** `RWF_NOWAIT` was confirmed on kernel 7.0 here; re-confirm on RHEL 9 (5.14) / Ubuntu 22.04 (5.15). If a target kernel/fs does not surface `-EAGAIN` (e.g. a filesystem that always punts), `adaptive` must degrade to `direct` or `standard` — gate it behind a runtime probe (mirror §11), never assume.

### D.6 When NOT to use it (honest bounds)

- **Hot set page-cache-resident + ChunkCache bypassed/undersized:** `direct` would convert cheap RAM hits into device reads — use `adaptive` or `standard`.
- **Large sequential warm reads:** plain `standard`/`mmap` beats io_uring ~3× (D.1) — do not route these through io_uring at all.
- **Writes:** no measured benefit — out of scope for this mode.

### D.7 Validation plan (gates P5)

1. **64 KiB cold QD sweep** (`-p chunk=65536 -p qd=1,4,8,16,32,64,128`) to locate the bandwidth knee and set the default `io_uring_queue_depth` for large reads (D.2 predicts ≈8–32).
2. **Mixed warm/cold workload** at controlled page-cache-residency ratios, comparing end-to-end read throughput **and p99** for `standard` vs `direct` vs `adaptive` — P5 ships only if `adaptive` beats **both** pure modes on the mix.
3. **Hot-path no-regression** microbench: `adaptive` vs `buffered`/`standard` on a 100%-resident workload (the `RWF_NOWAIT` probe must be ~free).
4. **Correctness:** Harry + parameterized reader suites (§15) green for `adaptive`, exercising the partial-hit / short-read boundary.
5. **Kernel matrix:** re-confirm `RWF_NOWAIT` `-EAGAIN` semantics on the RHEL 9 / Ubuntu 22.04 floor kernels.

**Trigger:** execute P5 only if D.7-step-2 shows `adaptive` winning a realistic mixed workload; otherwise ship `direct` for compaction/scan (P3) and leave the general point-read path on `standard`. The design is recorded here so a future session can pick it up without re-deriving it.










