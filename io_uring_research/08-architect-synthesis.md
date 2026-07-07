Read complete. Below is the architect's synthesis. It is my final return value.

---

# io_uring FFM Library + `disk_access_mode=io_uring` — Architectural Synthesis

**Purpose:** guide the writing of the final implementation plan. This reconciles the 7 research reports with the existing draft (`IO_URING_DISK_ACCESS_MODE_PLAN.md`), fixes the draft's factual errors inline, and makes firm architectural calls. The through-line: build a **standalone, pure-FFM, zero-native-artifact io_uring library** in `org.apache.cassandra.io.uring` that is complete enough to stand alone (full op surface, registration, provided buffers, network ops) with **zero hard Cassandra dependencies** (everything Cassandra-specific behind SPIs), then bolt a **thin, revertible** `disk_access_mode=io_uring` adapter onto it.

**Corrections the final plan MUST absorb (verified across reports):**
- **`--enable-native-access=ALL-UNNAMED` is already present** at `conf/jvm25-server.options:115`. The draft §3.6/§5.6 ("must be added / not present today") is **stale**. Real gaps: `conf/jvm25-clients.options` and the ant/test/JMH harness (`build.xml:383`, add to `_jvm25_arg_items`).
- **Draft §3.4 opcode bug:** `IORING_OP_READ_FIXED` is **opcode 4**, not 21 (21 is `STATX`). `READ`=22, `READV`=1, `WRITE_FIXED`=5, `READV_FIXED`=60.
- **Kernel `Semver` is `com.vdurmont.semver4j.Semver` (LOOSE) + Guava `Range`**, not OSHI (draft §5.2 wrong).
- **`ChannelProxy.getChannel()` does not exist** (draft §3.5) — use `ChannelProxy.getFileDescriptor()` (`ChannelProxy.java:218`).
- **`disk_access_mode: direct` is rejected today** (`DatabaseDescriptor.java:685-687` throws). io_uring is genuinely new top-level read plumbing, not a `direct` variant.
- **No `index_access_mode` yaml knob** exists; `indexAccessMode` is a derived private field. Fallback must reset **both** `disk_access_mode` and `indexAccessMode`.
- **`DataComponent` is write-only**; read data-mode is chosen in `SortedTableReaderLoadingBuilder.java:65`.
- **`FileHandle.ioMode()` has a `default: throw new AssertionError`** (`FileHandle.java:467`) — a new enum value crashes unless a `case io_uring:` and a new `ChannelProxy.IOMode` constant are added.
- **BufferPool geometry:** `NORMAL_CHUNK_SIZE=128 KiB`, unit `2 KiB`, `MACRO_CHUNK_SIZE=8 MiB`; use `MemoryUtil.getAddress(ByteBuffer)` for the SQE addr.
- **SQPOLL privilege boundary:** 5.11 = `CAP_SYS_NICE`, 5.13+ = none (draft "pre-5.12" is imprecise).
- **FFM compiles only on JDK 25** (JDK 21 = preview, proven). CI compiles on 11/17/21/25 → **compile-exclusion + ServiceLoader boundary is mandatory, not stylistic.**

---

## 1. Layered module architecture + full class list

Package root `org.apache.cassandra.io.uring` = the **extractable core library** (zero Cassandra deps). Adapters that touch Cassandra internals live in existing packages (`io.util`, `cache`, `config`, `service`) and are marked **[ADAPTER — outside core]**. The core is 8 layers, bottom-up; each layer depends only on layers below it and on the SPI package.

### (a) Raw ABI / syscall layer — `o.a.c.io.uring.linux`
Depends on: JDK FFM only (`java.lang.foreign`). No layout knowledge beyond scalar args.

| Class | Responsibility | Deps |
|---|---|---|
| `Syscalls` | 3 fixed-arity downcall handles over libc variadic `syscall` (`firstVariadicArg(1)`, `captureCallState("errno")`), per-arch nrs **425/426/427** (arch-agnostic on x86-64+aarch64, jdk25-ffm report). `invokeExact` only. | FFM |
| `LibC` | Downcall handles for `mmap`/`munmap` (fixed 6-arg, not variadic), `eventfd`/`eventfd_write`, `getrlimit`, and `pread` (probe golden-oracle only). | FFM |
| `Errno` | errno constants (`EAGAIN,EINTR,EBUSY,ENOSYS,EPERM,EFAULT,EINVAL,ENOMEM,EMFILE,EOPNOTSUPP,ECANCELED,ETIME`) + `strerror` mapping; reads the leading capture segment `VarHandle` (`captureStateLayout().varHandle("errno")`). | FFM |
| `Arch` | Resolves `os.arch` (via injected property provider, not `System.getProperty`) → syscall-number table. Trivial today (generic ABI) but keeps future arches localized. | — |

### (b) Memory-layout / constants layer — `o.a.c.io.uring.abi`
Depends on: FFM. Pure data; no syscalls. **`structLayout` does NOT auto-pad — every gap is an explicit `paddingLayout`** (jdk25-ffm report).

| Class | Responsibility |
|---|---|
| `Layouts` | `StructLayout`s for `io_uring_params`(120B, `sq_off`@40/`cq_off`@80/`features`@20), `io_sqring_offsets`(40B), `io_cqring_offsets`(40B), `io_uring_sqe`(64B), `io_uring_cqe`(16B), `iovec`(16B), `__kernel_timespec`(16B, **not** libc timespec), `io_uring_probe`+`probe_op`, `io_uring_rsrc_register`, `io_uring_rsrc_update2`, `io_uring_buf_reg`, `io_uring_buf`, `io_uring_getevents_arg`(24B), `io_uring_sync_cancel_reg`. Startup **byteSize/byteOffset assertions** vs kernel ground truth (arch-layout guard). |
| `Opcodes` | `IORING_OP_*` 0..63 (`NOP=0,READV=1,WRITE=2,FSYNC=3,READ_FIXED=4,WRITE_FIXED=5,…,READ=22,WRITE=23,…,READV_FIXED=60`). |
| `SetupFlags` | `IORING_SETUP_*` incl. `SINGLE_ISSUER(1<<12)`, `DEFER_TASKRUN(1<<13)`, `CQSIZE`, `NO_SQARRAY(1<<16)`, `SQE128/CQE32`. |
| `FeatureFlags` | `IORING_FEAT_*` (`SINGLE_MMAP,NODROP,SUBMIT_STABLE,EXT_ARG,…`). |
| `EnterFlags` | `IORING_ENTER_*` (`GETEVENTS,SQ_WAKEUP,EXT_ARG,REGISTERED_RING,…`). |
| `SqeFlags` | `IOSQE_*` (`FIXED_FILE,IO_LINK,IO_HARDLINK,IO_DRAIN,ASYNC,BUFFER_SELECT,CQE_SKIP_SUCCESS`). |
| `CqeFlags` | `IORING_CQE_F_*` + `IORING_CQE_BUFFER_SHIFT=16`. |
| `SqCqFlags` | `IORING_SQ_{NEED_WAKEUP,CQ_OVERFLOW,TASKRUN}`, `IORING_CQ_EVENTFD_DISABLED`. |
| `RegisterOps` | `IORING_REGISTER_*` (0..37) + `IORING_REGISTER_USE_REGISTERED_RING`. |
| `MmapOffsets` | `IORING_OFF_{SQ_RING=0,CQ_RING=0x8000000,SQES=0x10000000,PBUF_RING=0x80000000}`, `PROT/MAP_*`. |
| `Sqe` | Thin accessor over a 64B `MemorySegment` slice using **offset-based `JAVA_*_UNALIGNED` VarHandles** (opcode@0, flags@1, ioprio@2, fd@4, off@8, addr@16, len@24, rw_flags@28, user_data@32, buf_index@40). |
| `Cqe` | Accessor over 16B slice: user_data@0, res@8 (`<0 ⇒ -errno`), flags@12. |

### (c) Safe ring layer — `o.a.c.io.uring.ring`
Depends on: (a),(b). **Single-threaded per ring.** This layer owns the barrier correctness — the #1 corruption risk (kernel-abi §8, liburing §4).

| Class | Responsibility | Deps |
|---|---|---|
| `Ring` | Owns ring fd + mmap'd SQ/CQ/SQE segments (2 mmaps if `SINGLE_MMAP`, else 3), cached `sq_off`/`cq_off` ints, `Arena` (confined or shared per model), features record. `AutoCloseable` → munmap + close fd. | Syscalls,LibC,Layouts |
| `SubmissionQueue` | `getSqe()` (local cursor, returns null when full), `flushSq()` (write `sq_array[tail&mask]`, then **`setRelease`** `ktail`), `sqRingNeedsEnter(flags)`, `sqReady()`, `sqSpaceLeft()`. `NO_SQARRAY` variant skips array. | Ring |
| `CompletionQueue` | `peekCqe()`, `peekBatch(int max, CqeConsumer)` / `forEachCqe`, `cqReady()` (`getAcquire ktail − khead`), `cqAdvance(n)` (**`setRelease`** khead), `cqRingNeedsFlush()` (`kflags & (CQ_OVERFLOW|TASKRUN)`). | Ring |
| `RingBarriers` | Named, audited helpers — `publishSqTail/loadSqHead/loadCqTail/advanceCqHead` — the only place acquire/release modes are open-coded (`JAVA_INT.varHandle()`, `getAcquire/setRelease`; `acquireFence()` before SQPOLL `kflags` read). "Never open-code these" (kernel-abi §8). | Ring |
| `RingSubmitter` | `submit()`, `submitAndWait(n)`, `submitAndWaitTimeout(n,ts)` composing flush + needs-enter + `io_uring_enter`; EINTR/EAGAIN/EBUSY retry policy; EXT_ARG path when `features&EXT_ARG`. | SubmissionQueue,CompletionQueue |
| `RingParams` | Immutable value: sq/cq entries, features, setup flags, mmap sizes. | Layouts |

### (d) Op-prep helpers — `o.a.c.io.uring.op`
Depends on: (b). **Full opcode surface** (cheap — field setters). This is what makes the library "standalone for arbitrary purposes."

| Class | Responsibility |
|---|---|
| `Prep` | Base `prepRw(sqe,op,fd,addr,len,off)` that **zeroes the full 64B** first (stale-union-byte leakage is the #1 hand-rolled-ring bug — liburing §7 H1). Plus the storage set (**[v1]**): `prepRead/prepWrite/prepReadv/prepWritev/prepReadFixed/prepWriteFixed/prepReadvFixed/prepFsync/prepNop/prepFallocate/prepFtruncate/prepFadvise`. |
| `PrepNet` | Network surface (**present for standalone completeness, unused by Cassandra v1**): `prepAccept[Direct]/prepMultishotAccept/prepConnect/prepSend[Zc]/prepRecv[Multishot]/prepSendmsg[Zc]/prepRecvmsg/prepSocket/prepShutdown/prepPollAdd[Multishot]/prepPollRemove`. |
| `PrepFs` | Filesystem/admin surface: `prepOpenat[2][Direct]/prepClose[Direct]/prepStatx/prepRenameat/prepUnlinkat/prepLinkat/prepMkdirat/prepSplice/prepTee`. |
| `PrepCtl` | Control ops: `prepTimeout[Remove/Update]/prepLinkTimeout/prepCancel(64)/prepFilesUpdate/prepMsgRing/prepProvideBuffers/prepRemoveBuffers`. |
| `TargetFixedFile` | Shared `file_index = (idx==ALLOC?ALLOC:idx+1)` logic for all `*_direct` variants. |

### (e) High-level async API — `o.a.c.io.uring.async`
Depends on: (c),(d),(f),(h). **The facade + completion dispatch.**

| Class | Responsibility | Deps |
|---|---|---|
| `IoUring` (facade) | Public entry: `open(IoUringConfig)`, sync + async + batched submit, `features()`, `probe()`, registration delegation, `close()`. Idiomatic wrapper mirroring liburing completeness. | all core |
| `IoUringEventLoop` | Owns one `Ring` + one `InflightRegistry` + poller loop + **MPSC foreign-submitter hand-off** + eventfd wakeup. `submit(...) → CompletableFuture`. Drives submit+reap on its owning thread only (thread-per-ring, prior-art §5, H6). | Ring,InflightRegistry,SPI |
| `InflightRegistry` | **The keep-alive.** `LongObjectHashMap<PendingOp>` keyed by a **monotonic 64-bit** `user_data` (never recycled — H5). Owns the buffer segment reference until the terminal CQE is reaped. Drained-empty gate for teardown (H1/H10). | — |
| `PendingOp` | `user_data`, buffer `MemorySegment`/`ByteBuffer` (strong ref), `CompletableFuture<Integer>`, fd, off, len, submitted-len; short-read resubmit state (H9). Pooled to avoid per-op allocation. | — |
| `CompletionDispatcher` | Reaps CQEs, decodes `res` (`<0`⇒-errno→exceptional complete; `0`⇒EOF; `0<res<len`⇒resubmit remainder), routes by `user_data`, `Reference.reachabilityFence(buffer)` after hand-off. Batched reap via `peekBatch`. | InflightRegistry |
| `Backpressure` | Per-ring permit gate (`io_uring_queue_depth`) acquired before submit / released on CQE; CQ sized ≥2×SQ so overflow is impossible in steady state (H4). Uses SPI-provided semaphore (checkstyle bans `j.u.c.Semaphore`). | — |
| `IoUringException` | Library's own typed error carrying errno; adapter translates to `FSReadError`/`CorruptSSTableException`. | — |

### (f) Registration / provided-buffers / fixed-files — `o.a.c.io.uring.reg`
Depends on: (a),(b),(c).

| Class | Responsibility |
|---|---|
| `BufferRegistrar` | `registerBuffers/registerBuffersSparse/registerBuffersUpdate/unregisterBuffers` (`REGISTER_BUFFERS2=15` preferred). **`getrlimit(RLIMIT_MEMLOCK)` pre-check**; `ENOMEM/EPERM` → caller falls back to unregistered `READ` (H14). |
| `FileRegistrar` | `registerFiles/registerFilesSparse/registerFilesUpdate/unregisterFiles` (`REGISTER_FILES2=13`); stable slot→fd map for ring lifetime; enables `IOSQE_FIXED_FILE` (H7 fd-lifetime safety). |
| `ProbeTable` | `REGISTER_PROBE=8` → `Capabilities.supports(opcode)`. **Runtime opcode gating** (never trust compile header — kernel-abi preamble). |
| `RingFdRegistrar` | `REGISTER_RING_FDS=20` → `ENTER_REGISTERED_RING` (latency opt, needs `FEAT_REG_REG_RING`). |
| `ProvidedBufferRing` | `setupBufRing/registerBufRing/BufRing.{init,add,advance,cqAdvance}/free` (**[NH]**, socket/streaming; out-of-scope for pread path but present for standalone completeness). |
| `SyncCancel` | `REGISTER_SYNC_CANCEL=24` for shutdown drain. |

### (g) Availability / probe / feature detection — `o.a.c.io.uring.probe`
Depends on: (a),(b),(c),(h-logger).

| Class | Responsibility |
|---|---|
| `IoUringAvailability` | The hardened probe (prior-art §2): (1) **read `/proc/sys/kernel/io_uring_disabled`** (≠0 ⇒ unavailable, avoids touching a filtered syscall); (2) `io_uring_setup(1,&params)`, treat `ENOSYS/EPERM/EACCES` as unavailable→fallback; (3) **exercise a real op** — submit `NOP`/`READ` on a scratch fd and reap its CQE (gVisor allows setup, fails ops); (4) record `Capabilities`. **SIGSYS-on-KILL-seccomp caveat**: only ever probe when io_uring is explicitly requested; optionally run the first raw probe in a short-lived forked helper so a `SCMP_ACT_KILL` policy kills the probe, not the daemon. Cache once, never re-probe. |
| `Capabilities` | Immutable: available?, kernel features bitset, supported opcodes, memlock ceiling, single-mmap?, nodrop?, ext-arg?. |

### (h) SPI seams — `o.a.c.io.uring.spi`
Depends on: nothing. The extraction boundary. Core references **only** these interfaces.

| Interface / value | Responsibility | Cassandra impl **[ADAPTER]** | Standalone default |
|---|---|---|---|
| `BufferAllocator` | `ByteBuffer acquire(int size, int alignment); void release(ByteBuffer); long address(ByteBuffer)` | wraps `BufferPools.forChunkCache()` + `MemoryUtil.getAddress` | `BufferUtil.allocateDirectAligned` (Agrona) |
| `UringLogger` | slf4j-shaped SPI (or just use slf4j directly — it's an extractable dep) | slf4j | slf4j |
| `UringStatsListener` | `onSubmit/onComplete(latency)/onFallback/inflightGauge/onOverflow` | forwards to `CassandraUringMetrics` | no-op / `LongAdder` |
| `RingThreadFactory` | supplies poller thread(s) | `executorFactory().infiniteLoop(...,UNSAFE)` + `NamedThreadFactory` | plain daemon thread |
| `PermitGate` | backpressure semaphore (avoids checkstyle `Semaphore` ban leaking) | `utils.concurrent.Semaphore` | `j.u.c.Semaphore` |
| `IoUringConfig` | value object: queueDepth, pollerThreads, sqpoll, registeredBuffers, fallbackOnUnavailable, blockSize, minKernel | populated from `Config`/`DatabaseDescriptor` | builder defaults |

### Cassandra adapter classes — **[OUTSIDE core library package]**

| Class | Package | Responsibility |
|---|---|---|
| `IoUringChunkReader` | `io.util` | `implements ChunkReader`; `readChunk(pos,buf)` submits to the event loop and (Strategy C) `future.get()` on a virtual thread. Compressed + uncompressed variants (mirrors `SimpleChunkReader`/`CompressedChunkReader.{Standard,Direct}`). Translates `IoUringException`→`FSReadError`/`CorruptSSTableException`. |
| `IoUringChannelProxy` | `io.util` | `ChannelProxy` subtype exposing raw fd (via `getFileDescriptor()`); optional `IOMode.IO_URING`. |
| `IoUringManager` | `service` (or `io.util`) | Singleton lifecycle: `start()` after `runStartupChecks()` (`CassandraDaemon.java:275`), `shutdown()` via `StorageService.addPreShutdownHook` (`:4091`). Owns the event-loop pool + carrier pool + SPI wiring + JMX. |
| `CassandraBufferAllocator` / `Slf4jUringLogger` / `CassandraUringMetrics` / `CassandraRingThreads` | `io.util` | SPI impls binding core → `BufferPool`/slf4j/`CassandraMetricsRegistry`/`ExecutorFactory`. |
| `checkIoUringAvailability` | `service.StartupChecks` | `StartupCheck` adding kernel-floor (`≥5.6` via vdurmont `Semver`+Guava `Range`) + fallback policy on top of `IoUringAvailability`. |
| enum + resolution + dispatch edits | `config`, `io.util` | `Config.DiskAccessMode.io_uring`; `DatabaseDescriptor` branches; `FileHandle.ioMode()`+`complete()` cases. |
| `META-INF/services/...AsyncReadProvider` | `src/resources` | ServiceLoader registration (present only in JDK25 jar; needs a rat `<exclude>`). |

---

## 2. Public API sketch

Idiomatic Java mirroring liburing completeness. Note the **checkstyle constraint**: `java.util.concurrent.CompletableFuture` is banned in-tree — either annotate `// checkstyle: permit this import` in the library (recommended, keeps the API JDK-standard and extractable) or return a library-owned `IoFuture`. I show `CompletableFuture` as the extractable choice.

```java
// ---- Facade (Group 1/3 liburing) ----
public final class IoUring implements AutoCloseable {
    public static IoUring open(IoUringConfig cfg);              // setup + mmap; reads params.features
    public static Capabilities probeCapabilities();             // static, cheap, cached
    public int features();
    public boolean supports(int opcode);                        // via REGISTER_PROBE
    public SubmissionQueue sq();  public CompletionQueue cq();  // low-level escape hatch
    @Override public void close();                              // drain -> munmap -> close fd

    // ---- Async (Group 2/3/6) ----
    public CompletableFuture<Integer> read (int fd, long off, ByteBuffer dst);
    public CompletableFuture<Integer> readFixed(int fixedIdx, long off, ByteBuffer dst, int bufIndex);
    public CompletableFuture<Integer> readv(int fd, long off, ByteBuffer[] dsts);
    public CompletableFuture<Integer> write(int fd, long off, ByteBuffer src);
    public CompletableFuture<Void>    fsync(int fd, boolean dataSync);
    // batched: prep many, submit once
    public Batch batch();                                       // fluent builder
}
public interface Batch {                                        // amortizes one io_uring_enter over N ops
    Batch read(int fd, long off, ByteBuffer dst, CompletableFuture<Integer> out);
    int submit();                                               // returns # SQEs consumed
    int submitAndWait(int minComplete);
}

// ---- Low-level ring (Group 2/3/4) — mirrors liburing get_sqe/submit/reap ----
public final class SubmissionQueue {
    public Sqe getSqe();                                        // null when full
    public int spaceLeft();  public int ready();
}
public final class Sqe {                                        // op-prep, base + full surface
    public Sqe prepRw(int op, int fd, long addr, int len, long off);   // zeroes 64B first
    public Sqe prepRead(int fd, long addr, int len, long off);
    public Sqe prepReadFixed(int fd, long addr, int len, long off, int bufIndex);
    public Sqe prepReadv(int fd, long iovecsAddr, int nr, long off);
    public Sqe prepFsync(int fd, int fsyncFlags);
    public Sqe setData64(long userData);  public Sqe setFlags(int iosqeFlags);
    // ... full prep surface (net/fs/ctl) available via op.Prep* helpers
}
public final class CompletionQueue {
    public long peekCqe();                                      // 0 == none; else segment offset
    public int  forEachCqe(CqeConsumer c, int max);            // batch reap
    public int  ready();
    public void advance(int n);                                // setRelease khead
}
@FunctionalInterface public interface CqeConsumer { void accept(long userData, int res, int flags); }

// ---- Registration (Group 5) ----
public final class Registrar {
    public void registerBuffers(ByteBuffer[] slabs);           // getrlimit precheck; ENOMEM -> caller falls back
    public void registerBuffersSparse(int nr);
    public void registerFiles(int[] fds);  public void registerFilesSparse(int nr);
    public void registerFilesUpdate(int offset, int[] fds);
    public void unregisterBuffers();  public void unregisterFiles();
    public void registerRingFd();                              // ENTER_REGISTERED_RING
    public BufRing setupBufRing(int entries, int bgid, int flags);   // [NH] provided buffers
}

// ---- Event loop (async facade internals, thread-per-ring) ----
public final class IoUringEventLoop implements AutoCloseable {
    public CompletableFuture<Integer> submitRead(int fd, long off, ByteBuffer dst);  // MPSC hand-off if foreign thread
    public void wakeup();                                       // eventfd, not NOP
    @Override public void close();                             // drain-before-close
}
```

Sync convenience for Strategy C lives in the **adapter** (`IoUringChunkReader`): `int n = loop.submitRead(fd,off,buf).get();` — textually synchronous, unmounts the virtual-thread carrier.

---

## 3. Comprehensive test / validation plan

Concrete targets. Unit/property/adapter tests under `test/unit/org/apache/cassandra/io/uring/…`; benches under `test/microbench/org/apache/cassandra/test/microbench/` (FQN must contain `microbench`, class must end in `Bench` — build report §2). All FFM tests gate at runtime with three `assumeTrue`s: `FBUtilities.isLinux`, `Runtime.version().feature()>=25`, `IoUringAvailability.check().isAvailable()`.

| Category | Test class(es) | What it asserts | Tooling |
|---|---|---|---|
| **Golden-oracle unit** | `IoUringGoldenReadTest` | byte-for-byte equality of ring `READ` vs `FileChannel.read`/`pread` across offsets/lengths/EOF/**>4 GiB**, on **tmpfs** (fast) and a real block device (O_DIRECT path). Catches ABI/layout errors immediately. | JUnit + tmpfs |
| **ABI layout guard** | `IoUringLayoutTest` | `Layouts.*.byteSize()`/`byteOffset()` == kernel ground truth (params=120, sqe=64, cqe=16, sq_off@40/cq_off@80); run on **x86-64 AND aarch64**. | JUnit + `Assume` per arch |
| **Barrier / ring cycle** | `IoUringBindingTest`, `IoUringRingCycleTest` | setup→enter→exit round-trip; get_sqe-full→submit→reap; `cqReady`/`sqSpaceLeft` accounting; short-read resubmit loop. | JUnit |
| **Property / fuzz** | `IoUringPropertyTest` | `qt()`/quicktheories random op sequences (mixed offset/len, interleaved submit/reap, forced short reads, injected cancels, ring-full); invariants: golden bytes + no lost/duplicated `user_data`. | quicktheories + `FakeIoUringRing` |
| **Fault injection** | `IoUringCqeErrorTest`, `IoUringAlignmentTest`, `IoUringBackpressureTest`, `IoUringFallbackTest`, `IoUringMemlockTest` | misalignment→`EINVAL` (CQE, not submit); `setrlimit(RLIMIT_MEMLOCK,small)`→`ENOMEM`→unregistered fallback; `EBUSY`/ring-full retry; negative `res`(`-EIO`)→`FSReadError`; `EINTR` retry; `ENOSYS`→fallback to `standard` **and** reset `indexAccessMode`; CRC mismatch→`CorruptSSTableException`. | `FakeIoUringRing` + Byteman `Injections` + `ListenableFileSystem` |
| **Leak detection** | `IoUringLeakTest` | after teardown: `InflightRegistry.size()==0`, `/proc/self/fd` count returns to baseline, Arena/segment alloc==free, RSS stable; a growing `user_data` map = lost-completion (H4). Poisoned-buffer test for write-after-free (H1). | JUnit + `/proc` polling |
| **Soak / stress** | `IoUringSoakTest` (test/long or test/burn) | sustained high QD, many rings, long duration; steady-state correctness + resource return to baseline. | test/long harness |
| **JMH micro-bench** | `IoUringChunkReaderBench` (latency p50/p99/p999, `Mode.SampleTime`), **`IoUringBatchedReadThroughputBench`** (throughput vs QD {1,4,8,16,32,64,128} — **the Gate-0 bench**), `CompactionReadModeBench`, `ChunkCacheMissLatencyBench` | io_uring vs **pread vs mmap vs O_DIRECT** at chunk {4,16,64,256 KiB}; registered vs unregistered; force misses via `NativeLibrary.trySkipCache`. **Study JUring's regression finding** (FileChannel wins ≥20 writers/64 KiB) — Gate 0 must be honest. | `ant microbench`, `@Fork(jvmArgsAppend="--enable-native-access=ALL-UNNAMED")` |
| **Adapter parameterized** | add `io_uring` to `CompressedRandomAccessReaderTest` (`:188` hook), `RandomAccessReaderTest`, `SSTableReaderDataReaderTest`; `DatabaseDescriptorTest.testIoUringDiskAccessMode`; `IoUringTestUtils` beside `DirectIoTestUtils` | reader parity across access modes; resolution correctness. | `@RunWith(Parameterized.class)` |
| **dtest** | `IoUringRepairTest`, `IoUringCompactionReadTest`, `IoUringStreamingTest`, `IoUringDiskFailurePolicyTest` | `.set("disk_access_mode","io_uring")`; no `FSReadError` in logs; row correctness post-compaction; `disk_failure_policy=stop` on injected error. Needs a kernel-check bypass property (like `IGNORE_KERNEL_BUG_1057843_CHECK`). | in-JVM dtest |
| **Differential oracle** | `HarryIoUringReadTest` | identical `HistoryBuilder` under `io_uring` vs `standard` → identical `QuiescentChecker` model state. Any divergence = bug. | Harry |
| **Availability gating** | `IoUringAvailabilityTest`, plus a CI cell running io_uring tests with **`--illegal-native-access=deny`** | sysctl-file path, setup-EPERM path, exercise-real-op path; deny-cell proves `--enable-native-access` stays wired. | JUnit + CI matrix |
| **Simulator negative** | assertion in sim harness | `io_uring` is **never** requested under simulation (stays on `standard`, `ClusterSimulation.java:885`). | simulator |

**Kernel-free CI:** the `FakeIoUringRing` (programmable partial fills / `-errno` / overflow) is what keeps unit+property+fault coverage green on non-Linux/non-25 cells and in the deterministic simulator. It is the most important single test asset.

---

## 4. Phased delivery with go/no-go gates

Reconciles the draft's Strategy A/B/C: **C (virtual-thread mounting) is the primary vehicle**, B (readahead) a scan/compaction optimization, A (CPS) held in reserve. Every phase is independently revertible.

| Phase | Deliverable | Files touched | Gate metric | Revert story |
|---|---|---|---|---|
| **P0 — Binding proven in isolation** | Core layers (a)–(d),(g) + minimal (e)/(f); `FakeIoUringRing`; golden-oracle + ABI-layout + property tests; JMH benches. Add `--enable-native-access` to `build.xml:383` + `jvm25-clients.options`. Compile-exclusion (`ffm.src.excludes`) + ServiceLoader skeleton. | new `io.uring.*`; `build.xml`; `conf/jvm25-clients.options`; rat `<exclude>` | **GATE 0:** io_uring beats `direct`/O_DIRECT on cache-cold NVMe reads at QD≥16, and does not badly regress single-read latency. **If not, STOP.** | Delete package; no core wiring exists yet. |
| **P1 — async facade** | Full (e) event loop: thread-per-ring, `InflightRegistry`, `CompletionDispatcher`, `Backpressure`, MPSC hand-off, eventfd wakeup, drain-before-close. SPI impls stubbed with standalone defaults. | `io.uring.async.*`, `io.uring.spi.*` | **GATE 1a:** soak/leak clean (registry drains, fd/RSS baseline); fault matrix green; batched-submit throughput scales with QD in isolation. | Facade unused by Cassandra; revert = drop async package. |
| **P2 — ChunkReader/ChunkCache seam** | `IoUringChunkReader`+`IoUringChannelProxy` **[ADAPTER]**; `Config` enum + `DatabaseDescriptor` branches + `FileHandle.ioMode()`/`complete()` cases + new `ChannelProxy.IOMode`; `checkIoUringAvailability`; SPI adapters (`BufferPool`/slf4j/metrics/threads); yaml docs. Decide sync `LoadingCache`+Loom vs `AsyncLoadingCache` (measure). | `Config.java:1355`, `DatabaseDescriptor.java:674-708`, `FileHandle.java:450-536`, `ChannelProxy.java:47-51`, `StartupChecks.java:123-141`, `cache/ChunkCache.java`, `cassandra.yaml` | **GATE 2:** correctness parity (Harry + parameterized + dtest green); macro read-heavy p99 ↓ on cache-cold NVMe; **no regression** on hot-cache. | Single enum value + branches; setting `disk_access_mode` back to `standard` fully disables (must reset both mode + `indexAccessMode`). |
| **P3 — registered buffers/files + integrated mode** | (f) registration wired into the read path; `compaction_read_disk_access_mode: io_uring`; Strategy B readahead for scans; `disk_access_mode=io_uring` as a first-class production mode with virtual-thread mounting per draft §5.3. | `io.uring.reg.*`, `SSTableReader.canReuseDfile:1469`, compaction scanners, `IoUringManager` | **GATE 3:** compaction throughput / scan MB/s ↑ with registered buffers vs P2; RLIMIT_MEMLOCK fallback verified. | Config flags `io_uring_registered_buffers=false` disables registration; mode revert as P2. |
| **P4 — (optional) CPS hot path** | Strategy A async iterators, **only** if P2/P3 data shows virtual-thread suspension overhead is the residual bottleneck. ~30–40 files. | `Rebufferer`/`ChunkReader` + iterator stack | proven residual bottleneck | Large; gated behind a proven need — do not start speculatively. |

**Note:** the draft folds "async facade" and "ChunkReader seam" together; I split them (P1/P2) so the facade's memory-safety invariants (H1/H8/H10) are proven under soak **before** any Cassandra data flows through it.

---

## 5. Extraction plan (mirror `accord`)

The `accord` submodule is the template (build report §3): `modules/accord`, own Gradle, built by `.build/build-accord.xml` (`gradlew clean build publishToMavenLocal`), consumed as a resolver jar, rat/checkstyle disabled in its own Gradle and re-run by the parent via `.build/rat-include-accord.sh`.

**Phase 0 (now): keep source in-tree, extraction-ready.** Source under `src/java/.../io/uring`, compiled only on JDK 25 via `ffm.src.excludes`. **All** Cassandra coupling flows through the (h) SPI package + ServiceLoader. No `Config`/`DatabaseDescriptor`/`BufferPool`/`System.getProperty`/`java.io.File` reference anywhere inside `io.uring.*`. This alone makes later extraction a mechanical move.

**Phase X (later): lift to `modules/io-uring`.**
1. `git submodule add <repo> modules/io-uring`; add to `.gitmodules`; set `iouring.dir=modules/io-uring` in `build.xml` (mirror `accord.dir` at `:115`).
2. New `.build/build-io-uring.xml` modeled on `build-accord.xml`, but its Gradle **pins `sourceCompatibility=targetCompatibility=25`** (accord uses 11) and the **whole module build is skipped when the outer JDK<25** (analogous to `-Dno-build-accord`). Wire into `_build_subprojects` (`build.xml:774`).
3. `.build/rat-include-io-uring.sh` (parent `git ls-tree` only sees the submodule pointer); disable the module's own rat/checkstyle in its Gradle.
4. Module layout: `src/main/java/org/apache/cassandra/io/uring/{linux,abi,ring,op,async,reg,probe,spi}` — **or** rename root to a neutral package (e.g. `dev.uring` / `io.uring.ffm`) if published as a general artifact; keep `org.apache.cassandra.io.uring` if it stays Cassandra-namespaced. Decide at extraction time (open question).
5. Minimal deps: JDK 25 (FFM) + slf4j + Agrona (for the standalone `BufferAllocator` default) only. No Cassandra, no Netty, no liburing, no JNI `.so`.
6. Every `.java` carries the ASF header; the `META-INF/services` file gets a rat `<exclude>`. Apache-2.0 license file at module root.
7. Cassandra consumes the published `io-uring-*.jar` via the resolver; the adapter classes (`IoUringChunkReader` etc.) and SPI impls stay in Cassandra's tree.

---

## 6. Open questions (prioritized — these are the clarifying questions for the user)

1. **How fully-featured for v1: storage-only vs storage+network?** The op-prep surface (§1d) is nearly free to include in full, so I recommend **shipping the complete prep surface + registration + buf-rings in the library** but wiring **only the storage read subset** (`READ/READV/READ_FIXED/READV_FIXED/FSYNC`) into Cassandra. Confirm: is the intent a genuinely general-purpose library (justifying the full async ergonomics for sockets/multishot/provided-buffers), or is "self-contained" satisfied by full *prep* coverage with a storage-focused *async facade*? This materially changes (e) and (f) scope.
2. **Pure-syscall FFM vs optional C shim?** All reports converge on **pure raw-`syscall` FFM, no shim** (no native toolchain/per-arch packaging/binary-signing the project has never had; unique among Java bindings except PanamaUring). Confirm we drop draft §3.1 Option B entirely, rather than "try A, fall back to B."
3. **Minimum kernel floor + what's in scope: registered buffers, SQPOLL, SINGLE_ISSUER/DEFER_TASKRUN?** Baseline **5.6** (`IORING_OP_READ`). Registered buffers/files = **P3, opt-in, RLIMIT_MEMLOCK-gated**. SQPOLL = **opt-in, off by default** (burns a core; 5.11 `CAP_SYS_NICE`/5.13 none). `SINGLE_ISSUER|DEFER_TASKRUN` on ≥6.1 = big latency win for the one-poller model but absent on RHEL 9/Ubuntu 22.04 — **feature-gated opt-in**. Confirm these defaults.
4. **Concurrency model: thread-per-ring vs ring-per-core?** I recommend **thread-per-ring** (bounded pool N≈`concurrent_reads`/CPU count, each owning one ring, MPSC foreign hand-off, virtual threads as suspension vehicle) and explicitly **NOT** Seastar shard-per-core (Cassandra isn't share-nothing) and **NOT** ring-per-virtual-thread (FastThreadLocal thrash). Confirm.
5. **Publish/extract now or later?** Recommend **later** (in-tree behind SPI first; extract post-Gate-2 once the API has stabilized under real load). Confirm, and if "later," confirm the package namespace choice for eventual publication (§5 step 4).
6. **How aggressive is the read-path async rewrite?** Recommend **Strategy C only** for P2/P3, holding Strategy A (CPS, ~30–40 files) strictly in reserve behind a proven bottleneck (P4). Confirm we do not attempt CPS speculatively.
7. **(Scope-adjacent) Does io_uring+O_DIRECT inherit the ext4 6.1.64–6.1.66 bug?** `checkKernelBug1057843` today covers **write paths only** (`getDirectIOWritePaths()`); O_DIRECT *reads* are already uncovered. If the bug affects reads, a new read-paths hookup is needed. Confirm whether v1 uses O_DIRECT reads at all (buffered io_uring reads sidestep this entirely).

---

## 7. Completeness critique — what a skeptic attacks

- **The win may not exist, and the honest data may be embarrassing.** JUring's own JMH shows `FileChannel` *beating* io_uring at ≥20 writer threads / 64 KiB; ScyllaDB measured only ~5% over tuned linux-aio on Optane. io_uring only helps **cache misses that reach the device** — hot reads never syscall. Gate 0 is load-bearing and must compare against **O_DIRECT and mmap**, not just buffered pread, at realistic QD. The research does **not** contain an actual Cassandra-workload number; that gap is the single biggest project risk.
- **Memory safety is enforced by convention, not the compiler.** Rust bindings get H1/H8/H10 for free via `!Send`/ownership; this library must hand-enforce "kernel writes after you think you're done" via the `InflightRegistry` + drain-before-close + memory-safe-cancel. A skeptic will hunt for the one path that frees/reuses a buffer or `munmap`s before the terminal CQE — under cancellation, teardown, or short-read resubmit. The poisoned-buffer leak test is the only real defense; specify it precisely.
- **Virtual-thread mounting is asserted, not measured.** Draft Strategy C assumes carriers unmount cleanly and FastThreadLocal thrash is avoidable by "keeping buffer alloc on carriers." Neither is validated. Pinning inside FFM frames, `synchronized` in the read path, and `BufferPool.LocalPool` thrash from ephemeral vthreads are all unproven. Loom gives *waiting*, not *batching* — if reads arrive one-at-a-time per vthread, the shared ring never batches and the whole thesis collapses. The MPSC-into-shared-ring batching path needs an explicit microbench.
- **Operational availability is worse than the draft admits.** Docker default seccomp → EPERM (the common containerized case); `io_uring_disabled=2` on hardened images; gVisor allows setup but fails ops; **`SCMP_ACT_KILL` can SIGSYS-kill the JVM at probe time.** The draft's probe (setup + record features) is insufficient — it must read the sysctl file first, exercise a real op, and treat probing as potentially fatal. For a large fraction of deployments io_uring will be unavailable, so the fallback path is the *primary* path and must be flawless (including resetting `indexAccessMode`, which the draft omits).
- **CQ-overflow / lost-completion → query hangs.** A dropped or misrouted CQE means a future never completes → read hangs → timeout. Requires `NODROP` asserted (≥5.6 ✓), CQ≥2×SQ, monotonic never-recycled `user_data`, backpressure semaphore, and multishot-aware retirement. A skeptic probes the overflow-flush path (`EBUSY` + `CQ_OVERFLOW` bit) under saturation.
- **Simulator blind spot.** io_uring completions arrive off the deterministic scheduler, so the strongest correctness tool Cassandra has (the simulator) **cannot cover this path**. Coverage leans entirely on Harry + fault injection + the `FakeIoUringRing`. That's a real reduction in assurance for a critical-path reader.
- **Under-specified corners the reports leave open:** (a) exact O_DIRECT buffer-alignment story — `BufferPool`'s 2 KiB unit is 512- but not guaranteed 4096-aligned for 4Kn NVMe; needs either an aligned allocator or a taught pool unit, and this interacts with registered buffers. (b) `AsyncLoadingCache` vs sync-`LoadingCache`+Loom for in-flight dedup is left "measure in Phase 1" — but `ChunkCache`'s `ImmediateExecutor` means eviction `onRemoval→buffer.release()` could run on the poller thread; needs a dedicated Caffeine executor. (c) fd-lifetime vs SSTable close with in-flight reads (H7) — no concrete deferred-close design is specified. (d) ABI drift: the compile-host header exposes opcodes/features (through `NO_IOWAIT`, opcode 63) that don't exist on RHEL 9/Ubuntu 22.04 — every optional path must be `REGISTER_PROBE`-gated, and a skeptic will find the one place that trusts the header.