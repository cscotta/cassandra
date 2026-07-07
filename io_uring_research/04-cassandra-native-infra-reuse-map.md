Research complete. Here is the dense reuse map.

---

# io_uring FFM library — Cassandra infrastructure reuse map

Branch `cscotta/jdk25`, verified against the working tree (file:line current as of commit 52ed036ac8). Corrections to the existing `IO_URING_DISK_ACCESS_MODE_PLAN.md` and to the task-prompt assumptions are flagged **[CORRECTION]**.

## 1. Native fd extraction

| Need | Reuse | file:line |
|---|---|---|
| fd from `FileChannel` | `NativeLibrary.getfd(FileChannel)` → reads `sun.nio.ch.FileChannelImpl.fd` field reflectively, returns `-1` on failure | `utils/NativeLibrary.java:379-391` |
| fd from `FileDescriptor` | `NativeLibrary.getfd(FileDescriptor)` → `FILE_DESCRIPTOR_FD_FIELD.getInt(descriptor)` | `utils/NativeLibrary.java:398-414` |
| Public accessor on a ChannelProxy | `ChannelProxy.getFileDescriptor()` → `NativeLibrary.getfd(channel)` | `io/util/ChannelProxy.java:218-221` |
| Reflection field setup | static block: `FBUtilities.getProtectedField(FileDescriptor.class,"fd")` and `getProtectedField(Class.forName("sun.nio.ch.FileChannelImpl"),"fd")` | `utils/NativeLibrary.java:79-90`; helper `FBUtilities.java:865-877` (just `getDeclaredField`+`setAccessible(true)`) |

- **JDK 25 / add-opens:** `getfd(FileChannel)` reaches into `sun.nio.ch.FileChannelImpl`, so it needs **`--add-opens java.base/sun.nio.ch=ALL-UNNAMED`** — **already present** at `conf/jvm25-server.options:110`. `getfd(FileDescriptor)` only touches `java.io`, covered by `java.base/java.io` open at `:99`. Both `getProtectedField` sites run once in a static initializer.
- **Thread-safety:** both `getfd` methods read an immutable `Field` and pull an `int`/object once per call; no shared mutable state. Safe to call from any submit thread. The fd itself is stable for the channel's life.
- **[CORRECTION]** `ChannelProxy.getChannel()` (referenced in the plan §3.5 and the task) **does not exist**. The only public surface is `getFileDescriptor()` (`:218`) and `newChannel()` (`:149`, returns a new `ChannelProxy` sharing the same `FileChannel`). The private field is `ChannelProxy.channel` (`:55`). To get an fd for io_uring, go through `getFileDescriptor()`.

## 2. Buffer infrastructure & O_DIRECT alignment

**BufferPool** (`utils/memory/BufferPool.java`):
- Constants (`:131-135`): `NORMAL_CHUNK_SIZE = 128<<10` (128 KiB), `NORMAL_ALLOCATION_UNIT = NORMAL_CHUNK_SIZE/64 = 2 KiB`, `TINY_CHUNK_SIZE = 2 KiB`, `TINY_ALLOCATION_UNIT = 32 B`. **[CORRECTION]** `MACRO_CHUNK_SIZE = 64 * NORMAL_CHUNK_SIZE = 8 MiB` (`:389`) — the code comment "1 MiB" at `:388` is stale; the plan's "8 MiB slabs" is right.
- Macro-chunk base is allocated **page-aligned** via BufferPool's **own** private `allocateDirectAligned(int capacity)` (`:1099-1120`, aligns to `MemoryUtil.pageSize()`, typically 4096) — **not** Agrona's. Slices at 128 KiB (`NORMAL_CHUNK_SIZE`) boundaries stay page-aligned.
- Sub-chunk allocation "somewhat"-aligns by slot count (`Chunk.allocate` searchMask logic, `:1413-1444`): a 4-unit (8 KiB) allocation is page-aligned; a 32-unit (64 KiB, the typical chunk-cache read) starts on a 4-slot (8 KiB) boundary. So **for chunk-sized (≥4 KiB, power-of-two) reads the pool already yields ≥4096-aligned direct buffers** suitable for O_DIRECT; the plan's worry (§3.4) applies only to sub-page allocations.
- API: `get(int size, BufferType)` `:210`, `getAtLeast(int,BufferType)` `:218`, `tryGet(int)` `:227` (returns null when exhausted). `ON_HEAP` bypasses to `ByteBuffer.allocate`; direct goes to `localPool.get().get(size)`.
- `put(ByteBuffer)` `:245-251` → `LocalPool.put` `:815-830` → `put(buffer,chunk)` `:832`. **Cross-thread release is safe:** `chunk.free(buffer)` is a CAS on `freeSlots`; if the releasing thread isn't the chunk owner it skips the fast local-queue recycle and routes via `chunk.tryRecycle()` (`:863-870`) — correct, marginally slower. This validates the submit→completion→continuation 3-thread pipeline in the plan §4.2.
- `LocalPool` is `FastThreadLocal<LocalPool>` (`:172`, `io.netty.util.concurrent.FastThreadLocal`) — confirms the plan's virtual-thread-thrash caveat; keep buffer alloc on bounded carriers.
- The chunk-cache pool instance: `BufferPools.CHUNK_CACHE_POOL` (`utils/memory/BufferPools.java:39`, `new BufferPool("chunk-cache", FILE_MEMORY_USAGE_THRESHOLD, true)`).

**Block size / alignment helpers:**
- `FileUtils.getBlockSize(File directory)` `io/util/FileUtils.java:789-792` — memoized per-dir via `ConcurrentHashMap` `:787`; delegates to `blockSize(File)` `:811-816` → `Files.getFileStore(path).getBlockSize()`.
- Agrona: `org.agrona.BufferUtil.allocateDirectAligned(capacity, alignment)` + `org.agrona.BitUtil.align(value, alignment)`. Canonical usage: `DirectThreadLocalReadAheadBuffer.java:38` (`allocateDirectAligned(BitUtil.align(bufferSize, blockSize), blockSize)`) and `:45` (round `sizeToRead` up to block, set limit, trim after). Also `DirectCompressedSequentialWriter.java`, `DirectThreadLocalByteBufferHolder.java`. Agrona-aligned buffers are **slices** — clean the backing buffer via `((DirectBuffer)buf).attachment()` (`DirectThreadLocalReadAheadBuffer.java:57`).
- **Native address for the SQE `addr` field:** `MemoryUtil.getAddress(ByteBuffer)` `utils/memory/MemoryUtil.java:79-83` (Unsafe read of the direct-buffer address field) — reuse this instead of `MemorySegment.ofBuffer(bb).address()`. `MemoryUtil.pageSize()` `:74`.

## 3. Existing native-call patterns

- **[CONFIRMED] No `java.lang.foreign` / `MemorySegment` / `Linker` / `FunctionDescriptor` anywhere in `src/`** (grep returns nothing). This binding is the first FFM code in the tree.
- All current native calls go through **JNA** (`com.sun.jna`), not JNR. Structure: interface `NativeLibraryWrapper`; impls `NativeLibraryLinux` / `NativeLibraryDarwin`; facade `NativeLibrary` (`utils/NativeLibrary.java:43`) picks the impl by `osType` in a static block (`:96-103`) and exposes `isAvailable()` (`:171`, delegates to `wrappedLibrary.isAvailable()`).
- `NativeLibraryLinux` binds libc by `Native.register(com.sun.jna.NativeLibrary.getInstance("c", …))` in a static block (`:52-71`, degrades gracefully on `NoClassDefFoundError`/`UnsatisfiedLinkError`), then declares `private static native int …` methods (mlockall, fcntl, posix_fadvise, open, fsync, close, strerror, getpid; `:73-81`). This is the availability/degradation pattern to mirror for `IoUringAvailability`.
- **No C/.so shim in the tree:** `build.xml` has no `<cc>`/gcc/native-header target and there are **zero `.c` files** (excluding build/.git). So the plan's Option B (§3.1, ship `libcassandra_uring.so`) would be a brand-new build capability — nontrivial. Favor pure-`syscall` FFM (Option A).
- Jars available: `lib/jna-5.13.0.jar`, `jna-platform-5.13.0.jar`, `agrona-1.17.1.jar`, plus `jnr-ffi-2.2.13.jar`/`jnr-constants`/`jnr-a64asm`/`jnr-x86asm` (present but **not** referenced from `src/java`).

## 4. Threading / lifecycle

- **Poller loop:** `ExecutorFactory.infiniteLoop(String name, Interruptible.SimpleTask task, SimulatorSafe)` `concurrent/ExecutorFactory.java:182-185` (delegates to full form `:170` with `DAEMON, UNSYNCHRONIZED`). Enum `InfiniteLoopExecutor.SimulatorSafe {SAFE, UNSAFE}` `InfiniteLoopExecutor.java:55`; also `Interrupts {SYNCHRONIZED, UNSYNCHRONIZED}` `:58`, `SystemThreadTag` (DAEMON) from `ExecutorFactory`. Canonical caller: `BufferPool.java:199` — `executorFactory().infiniteLoop("LocalPool-Cleaner-"+name, this::cleanupOneReference, UNSAFE)`; returns `Shutdownable`/`Interruptible`, stopped via `shutdownAndWait(...)` `BufferPool.java:1625`. Global accessor `ExecutorFactory.Global.executorFactory()` `:200`. **Use `UNSAFE`** for io_uring pollers (completion arrives outside the simulator scheduler — see §7.6 of the plan).
- **Alternate lifecycle model (Netty epoll):** `net/SocketFactory.java` holds `EventLoopGroup` fields (`:181-184`), builds them via a `Provider` enum (`makeEventLoopGroup`, EPOLL vs NIO chosen at `:168` by `NativeTransportService.useEpoll()`), and shuts down with `shutdownGracefully(0,2,SECONDS)` per group in `shutdownNow()` `:283-288`. Good template for "start N rings, drain, close."
- **Init hook:** `CassandraDaemon.setup()` `service/CassandraDaemon.java:243`, calls `runStartupChecks()` at `:275` (which runs `DatabaseDescriptor.getStartupChecksConfiguration().verify()` `:459-463`). Start the `IoUringManager` right after `:275`.
- **Shutdown hook:** `StorageService.addPreShutdownHook(Runnable)` `service/StorageService.java:4091-4095` (list at `:329`, drained at `:3879` inside the JVM shutdown hook registered `:774`). `removePreShutdownHook` `:4104`.

## 5. StartupChecks & fallback

- `StartupChecks.DEFAULT_TESTS = ImmutableList.of(checkKernelBug1057843, …, checkDirectIOSupport, …)` `service/StartupChecks.java:123-141`; appended to `preFlightChecks` at `:160`; extend via `withTest(StartupCheck)` `:214`.
- Interface `StartupCheck` `service/StartupCheck.java:35` — `String name()` (`:44`), `void execute(StartupChecksConfiguration) throws StartupException` (`:56`), defaults `isConfigurable()` (`:63`), `isDisabledByDefault()` (`:76`). Each check begins `if (configuration.isDisabled(name())) return;`.
- **`checkDirectIOSupport`** `:872-909` — gates on `getCompactionReadDiskAccessMode()==direct || getBackgroundWriteDiskAccessMode()==direct`, then `findDirectIOUnsupportedLocations(getAllDataFileLocations())` `:911-927` (`FileUtils.isDirectIOSupported(dir)`), throws `ERR_WRONG_DISK_STATE`. Direct model to copy for `checkIoUringAvailability`.
- **`checkKernelBug1057843`** `:244-302` — `if (!FBUtilities.isLinux) return;` `:258`; builds `Range<Semver> affectedKernels = Range.closedOpen(new Semver("6.1.64",LOOSE), new Semver("6.1.66",LOOSE))` `:287-288`; `Semver kernelVersion = FBUtilities.getKernelVersion()` `:290`; `affectedKernels.contains(kernelVersion.withClearedSuffixAndBuild())` `:291`. Property-based bypass via `IGNORE_KERNEL_BUG_1057843_CHECK` `:263`.
  - **[CORRECTION]** `Semver` is **`com.vdurmont.semver4j.Semver`** (import `StartupChecks.java:53`), **not OSHI's** as the plan §5.2 and the task both state. `Range` is Guava `com.google.common.collect.Range` (import `:52`). For a `≥5.6` gate use e.g. `Range.atLeast(new Semver("5.6", LOOSE))`.
- `FBUtilities.getKernelVersion()` `utils/FBUtilities.java:1461-1463` → `SystemInfo.getKernelVersion()` `utils/SystemInfo.java:177-185` = OSHI `getVersionInfo().getBuildNumber()` wrapped in a vdurmont `Semver(version, LOOSE)`. Overridable in tests via `FBUtilities.setSystemInfoSupplier` `:147`.
- `StartupException` codes `exceptions/StartupException.java:26-29`: `ERR_WRONG_MACHINE_STATE=1`, `ERR_WRONG_DISK_STATE=3`, `ERR_WRONG_CONFIG=100`, `ERR_OUTDATED_SCHEMA=101`.
- **Fallback pattern to mirror:** `NativeTransportService.useEpoll()` `service/NativeTransportService.java:136-144` — boolean gate (property) AND availability probe; **warn** (not throw) when configured-but-unavailable on Linux (`:140-141`), return the AND. For io_uring, on unavailable-with-fallback: `logger.warn(...)` + `DatabaseDescriptor.setDiskAccessMode(DiskAccessMode.standard)` (setter exists, `DatabaseDescriptor.java:4059`).

## 6. Config plumbing

- Enum `Config.DiskAccessMode { auto, mmap, mmap_index_only, standard, legacy, direct }` `config/Config.java:1355-1367`. Add `io_uring` here (note the Javadoc contract at `:1363-1366` re: `checkKernelBug1057843`).
- Fields: `disk_access_mode` `:130`, `compaction_read_disk_access_mode` `:487`, `background_write_disk_access_mode` `:416`, `commitlog_disk_access_mode` `:486`, `concurrent_reads=32` `:251`.
- **Resolution** in `DatabaseDescriptor.applySimpleConfig`: main mode at `:674-693`, compaction-read at `:695-708`. Backing statics + getters: `getDiskAccessMode()` `:4052`, `setDiskAccessMode(mode)` `:4059`, `getIndexAccessMode()` `:4064`, `getCompactionReadDiskAccessMode()` `:3427`, `getBackgroundWriteDiskAccessMode()` `:3462`. Add the `io_uring` branch inside the `else` at `:689-692` (set `conf.disk_access_mode` + `indexAccessMode`).
  - **[CORRECTION]** `disk_access_mode: direct` is **explicitly rejected today** — `:685-687` throws `ConfigurationException("DiskAccessMode 'direct' is not supported")`. So `direct` is *not* a valid main read-path mode (it's only reachable for compaction-read `:699-701`, commitlog `:1842-1906`, and background-write `:3490`). io_uring will be the first non-mmap/standard main read mode; the FFM reader is genuinely new plumbing, not a variant of an existing `direct` read path.
- **FileHandle dispatch (the integration seam):** `FileHandle.Builder.complete()` `io/util/FileHandle.java:445-536` builds the `RebuffererFactory` by `diskAccessMode`: `mmap`→`MmapRebufferer`/`CompressedChunkReader.Mmap` (`:490-504`), else (standard/direct)→`CompressedChunkReader.Direct`|`.Standard` or `SimpleChunkReader` (`:505-524`), all wrapped by `maybeCached(reader)` `:538-543` → `chunkCache.wrap(reader)`. `ioMode()` `:450-469` maps mode→`ChannelProxy.IOMode {BUFFERED, DIRECT}`. Add an `io_uring` case in both `complete()` (build `IoUringChunkReader`) and `ioMode()`. Builder setter: `withDiskAccessMode(DiskAccessMode)` `:393` (default `standard` `:345`).
- **Reader template:** `SimpleChunkReader` (`io/util/SimpleChunkReader.java`, entire file, 75 lines) — `implements ChunkReader extends AbstractReaderFileProxy`; `readChunk(pos,buf){ buf.clear(); channel.read(buf,pos); buf.flip(); }` `:37-43`; `instantiateRebufferer` returns `BufferManagingRebufferer.Aligned/Unaligned` `:57-64`. `ChunkReader` interface (`io/util/ChunkReader.java:31`) is **required thread-safe** (`:29`): `readChunk(long,ByteBuffer)` `:39`, `chunkSize()` `:44`, `preferredBufferType()` `:50`, `releaseUnderlyingResources()` default `:52`.
- **ChunkCache seam:** `ChunkCache.wrap(ChunkReader)` `cache/ChunkCache.java:187-190` → `CachingRebufferer` (`:219`, rebuffer TOCTOU loop `:232-252`); `load(Key)` `:160-174` = `bufferPool.get(chunkSize, preferredBufferType)` → `readChunk` → `new Buffer`; Caffeine built with `.executor(ImmediateExecutor.INSTANCE)` `:150-156` (the sync `LoadingCache` the plan proposes swapping to `AsyncLoadingCache`).
- **cassandra.yaml:** `disk_access_mode` (commented) at `:459`; `compaction_read_disk_access_mode` block `:689-705`. Surface new scalars here.

## 7. JMX / metrics registration pattern

- **`BufferPoolMetrics`** `metrics/BufferPoolMetrics.java` — `TYPE_NAME="BufferPool"` `:29`; ctor `(String scope, BufferPool pool)` `:52` builds `MetricNameFactory factory = new DefaultNameFactory(TYPE_NAME, scope)` `:54`; registers via static-imported `CassandraMetricsRegistry.Metrics` (`:25`): `Metrics.meter(factory.createMetricName("Hits"))` `:56`, `Metrics.register(name, gauge)` for gauges `:60-66`. Instantiated per-pool inside `BufferPool` ctor: `this.metrics = new BufferPoolMetrics(name, this)` `BufferPool.java:197` (field `:146`, accessor `metrics()` `:1629`). **This is the exact template** for `IoUringMetrics(scope, ring)` exposing submits/completions/inflight/overflow/fallbacks as `Meter`/`Gauge`.
- **`ChunkCacheMetrics`** `metrics/ChunkCacheMetrics.java:36` extends `CacheMetrics implements StatsCounter`; ctor `super(TYPE_NAME, cache)` `:49` then `missLatency = Metrics.timer(factory.createMetricName("MissLatency"))` `:50` — template for a `Timer` on completion latency.
- Both are plain-object holders registered into the global `CassandraMetricsRegistry` (auto-exposed via JMX by `DefaultNameFactory`); no separate MBean interface needed.

## 8. Runtime env detection

- OS: `FBUtilities.isLinux` `utils/FBUtilities.java:128` (`OPERATING_SYSTEM.contains("linux")`, from `OS_NAME` property `:127`). Also `NativeLibrary.osType == OSType.LINUX` (`NativeLibrary.java:56,96-103`).
- Arch: `Architecture` `utils/Architecture.java` exposes `IS_UNALIGNED` `:44` and `BIG_ENDIAN` `:47` but **no x86-vs-aarch64 distinguisher**. For raw-syscall selection read the property directly: `CassandraRelevantProperties.OS_ARCH` `config/CassandraRelevantProperties.java:435` (`"os.arch"`). Note: io_uring syscall numbers **425/426/427 are identical on x86-64 and aarch64** (generic ABI), so arch detection is only needed if you later add a non-generic arch; it's not required for x86-64/aarch64 parity.
- JDK feature guard: no existing helper — use `Runtime.version().feature()` directly (guard FFM code to `>=25 && isLinux`, matching the JDK17-CI-compile constraint). No `Runtime.version()` feature-gate exists in `src/` to copy.

---

## SPI / extractability notes (keep the library free of Cassandra core deps)

To keep `org.apache.cassandra.io.uring` self-contained and extractable, hide these behind small interfaces and inject Cassandra's impls at the `FileHandle`/`IoUringManager` boundary:

- **Buffers** — don't hard-depend on `BufferPool`/`ChunkCache.Buffer`. Define `interface AlignedBufferAllocator { ByteBuffer acquire(int size, int alignment); void release(ByteBuffer); }`. Cassandra impl wraps `BufferPools.forChunkCache().get/put`; the address is taken with `MemoryUtil.getAddress` (or `MemorySegment.ofBuffer`) inside the library. Standalone impl uses `BufferUtil.allocateDirectAligned` (Agrona) — a dep the library can carry itself.
- **fd** — take a raw `int fd` (or a `ToIntFunction<FileChannel>`) at submit; the `NativeLibrary.getfd` reflection stays in Cassandra glue, not the library.
- **Threads** — accept a `ThreadFactory`/`Executor` for the poller rather than calling `executorFactory().infiniteLoop` internally; Cassandra passes a `NamedThreadFactory` + `UNSAFE` loop, standalone passes a plain daemon thread.
- **Config** — the library takes a plain `IoUringConfig` value object (queueDepth, pollers, sqpoll); Cassandra maps `Config` fields into it. No `Config`/`DatabaseDescriptor` reference inside the library.
- **Metrics** — define a tiny `IoUringStatsListener` (onSubmit/onComplete/onFallback/inflight gauge). Cassandra impl forwards to `IoUringMetrics` (Codahale via `CassandraMetricsRegistry.Metrics`); standalone impl is a no-op/`LongAdder`.
- **Errors** — library throws its own `IoUringException`/returns `-errno`; Cassandra glue translates to `FSReadError`/`CorruptSSTableException` at the `IoUringChunkReader` seam (mirroring `ChannelProxy.read` `:169-180`).
- **Availability/kernel probe** — library owns the `io_uring_setup(1,…)` + `ENOSYS`/`EPERM` probe and returns a capability record; Cassandra's `StartupCheck` adds the `FBUtilities.getKernelVersion()`/`Range<Semver>` policy on top.

## Consolidated corrections to the existing plan

1. **`--enable-native-access=ALL-UNNAMED` is ALREADY present** at `conf/jvm25-server.options:115` (with an explanatory comment `:113-114`). Contradicts plan §3.6/§5.6 ("must be added / not present today") **and the task prompt**. No JVM-option change needed for this.
2. **`disk_access_mode: direct` is rejected today** (`DatabaseDescriptor.java:685-687`); `direct` is not a live main-read-path mode. io_uring is net-new read plumbing.
3. **Kernel Semver is `com.vdurmont.semver4j.Semver` (LOOSE), Range is Guava** — not "OSHI Semver" as stated in plan §5.2 and the prompt.
4. **`ChannelProxy.getChannel()` does not exist** (plan §3.5 / prompt area 1) — use `getFileDescriptor()` `:218`.
5. **BufferPool sizes:** `NORMAL_CHUNK_SIZE=128 KiB` (not 64), unit `2 KiB`, `MACRO_CHUNK_SIZE=8 MiB`; the `:388` "1 MiB" comment is stale. Macro base is page-aligned by BufferPool's own `allocateDirectAligned(int)` `:1099`, distinct from Agrona's `BufferUtil.allocateDirectAligned` used by the Direct* writers/readers.
6. Prefer `MemoryUtil.getAddress(ByteBuffer)` `:79` for the SQE addr (already Unsafe-based, no extra FFM).

Key files: `utils/NativeLibrary.java`, `io/util/ChannelProxy.java`, `io/util/FileHandle.java`, `io/util/SimpleChunkReader.java`, `io/util/ChunkReader.java`, `cache/ChunkCache.java`, `utils/memory/BufferPool.java`, `utils/memory/MemoryUtil.java`, `io/util/FileUtils.java`, `io/util/DirectThreadLocalReadAheadBuffer.java`, `concurrent/ExecutorFactory.java`, `concurrent/InfiniteLoopExecutor.java`, `net/SocketFactory.java`, `service/NativeTransportService.java`, `service/StartupChecks.java`, `service/StartupCheck.java`, `service/CassandraDaemon.java`, `service/StorageService.java`, `config/Config.java`, `config/DatabaseDescriptor.java`, `metrics/BufferPoolMetrics.java`, `metrics/ChunkCacheMetrics.java`, `utils/FBUtilities.java`, `utils/SystemInfo.java`, `utils/Architecture.java`, `config/CassandraRelevantProperties.java` (all under `/home/cscotta/projects/cassandra/src/java/org/apache/cassandra/`).