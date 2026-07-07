I have verified all the core files. Here is the dense report.

---

# io_uring `disk_access_mode` — Verified Subsystem Report (branch `cscotta/jdk25`, commit 52ed036ac8)

All file:line refs below were read and confirmed today. Divergences from `IO_URING_DISK_ACCESS_MODE_PLAN.md` are flagged with **[PLAN-DELTA]**.

## 1. `Config.DiskAccessMode` enum — `src/java/org/apache/cassandra/config/Config.java:1355-1367`

Six values (verbatim):
```java
public enum DiskAccessMode
{
    auto,
    mmap,
    mmap_index_only,
    standard,
    legacy,

    /**
     * When adding support for Direct I/O, update {@link org.apache.cassandra.service.StartupChecks#checkKernelBug1057843}
     */
    direct
}
```
Only `direct` carries a javadoc — an explicit contract that adding a new device-touching mode must revisit `checkKernelBug1057843`. A new `io_uring` value slots in here. Plan §5.1 cites `1355-1367` — **correct**.

## 2. Every disk-access-mode knob: declaration, default, resolution

| Knob | Config field (file:line) | Default | Getter/Setter | Resolution site |
|---|---|---|---|---|
| `disk_access_mode` | `Config.java:130` | **`mmap_index_only`** | `DatabaseDescriptor.java:4052/4059` (`get/setDiskAccessMode`) | `DatabaseDescriptor.java:674-693` |
| `indexAccessMode` (**derived, not a yaml knob**) | `DatabaseDescriptor.java:225` (private static) | derived | `4064/4071` (`get/setIndexAccessMode`) | `674-692` (set alongside disk_access_mode) |
| `compaction_read_disk_access_mode` | `Config.java:487` | **`auto`** | `3427/3433` | `DatabaseDescriptor.java:695-708` |
| `commitlog_disk_access_mode` | `Config.java:486` | **`legacy`** | `3442/3448`; init `3455-3460` | `resolveCommitLogWriteDiskAccessMode` `1842-1887`; validate `1889-1912` |
| `background_write_disk_access_mode` | `Config.java:416` | **`standard`** | `3462/3468` | `initializeBackgroundWriteDiskAccessMode` `3497-3525` |

**[PLAN-DELTA]** There is **no `index_access_mode` yaml knob** (plan item #2 lists it as a knob). `indexAccessMode` is a *derived* private field, computed only inside the `disk_access_mode` block.

### 2a. `disk_access_mode` + indexAccessMode resolution — `DatabaseDescriptor.java:674-693` (verbatim logic)
```java
if (conf.disk_access_mode == auto || conf.disk_access_mode == mmap_index_only) {
    conf.disk_access_mode = standard;      indexAccessMode = mmap;      // 675-679
} else if (conf.disk_access_mode == legacy) {
    conf.disk_access_mode = hasLargeAddressSpace() ? mmap : standard;   // 680-683
    indexAccessMode = conf.disk_access_mode;
} else if (conf.disk_access_mode == direct) {
    throw new ConfigurationException("DiskAccessMode 'direct' is not supported");  // 685-688  <-- direct is REJECTED as a top-level data mode
} else {
    indexAccessMode = conf.disk_access_mode;   // 689-692  (standard/mmap)
}
```
- 32- vs 64-bit handling: `hasLargeAddressSpace()` (call at `:682`; impl `:4794`, checks `sun.arch.data.model == "64"`). Only `legacy` consults it; `auto`/`mmap_index_only` unconditionally pick `standard` data + `mmap` index.
- **`direct` is NOT a legal top-level `disk_access_mode`** (throws at 687) — it is only reachable via `compaction_read_disk_access_mode` / per-scan override / `background_write`. A new `io_uring` value must decide: legal top-level, or read-scan-only like `direct`.

### 2b. `compaction_read_disk_access_mode` resolution — `DatabaseDescriptor.java:695-708`
```java
if (auto == conf.compaction_read_disk_access_mode)  compactionReadDiskAccessMode = conf.disk_access_mode;   // 695-698
else if (direct == ...)                             compactionReadDiskAccessMode = direct;                  // 699-702
else throw new IllegalArgumentException("... (options: direct/auto) ...");                                   // 703-707
```
Only `auto`/`direct` accepted; **anything else throws `IllegalArgumentException`** (not `ConfigurationException`). io_uring needs an explicit branch here.

### 2c. commitlog (`1842-1912`) and background_write (`3497-3525`)
Both have closed allow-lists that **throw** on unknown modes (commitlog: auto→legacy/direct/standard/mmap juggling keyed on compress/encrypt + `disk_optimization_strategy==ssd` + `isDirectIOSupported`; background_write: only `standard`/`direct`). These are **write paths** — io_uring is read-only, so leaving them unsupported (plan §5.1) is consistent, but note they will *reject* an io_uring value with an exception if an operator sets it there.

## 3. Read-path call chain (bottom → top), verified file:line

| # | Layer | file:line | Notes |
|---|---|---|---|
| 1 | `FileChannel.read(buf, pos)` / `MappedByteBuffer` slice | `ChannelProxy.java:169-180` (`read`); mmap via `ChannelProxy.map` `194-204` | positional pread; wraps `IOException`→`FSReadError`. `// FIXME: consider wrapping in a while loop` at :173. |
| 1a | O_DIRECT open | `ChannelProxy.java:69-80` (`openOptions`: DIRECT→`{READ, ExtendedOpenOption.DIRECT}`), `IOMode` enum `47-51` | fd via `getFileDescriptor()` `218-221`→`NativeLibrary.getfd(channel)` (`NativeLibrary.java:379`). |
| 2 | `ChunkReader.readChunk(pos,buf)` | iface `ChunkReader.java:39`; `SimpleChunkReader.java:37-43`; `CompressedChunkReader.Standard:399`, `.Direct:296`, `.Mmap:474` | uncompressed→SimpleChunkReader; compressed→CompressedChunkReader.{Standard,Direct,Mmap}. |
| 3 | `Rebufferer.rebuffer(pos)→BufferHolder` | iface `Rebufferer.java:36`; `BufferManagingRebufferer.java:76-82` (no-cache); `MmapRebufferer.java:37-41`; `ChunkCache.CachingRebufferer.rebuffer` `ChunkCache.java:232-252` | **the sync seam.** Cache path: `cache.get(new Key(source,pos)).reference()` retry loop `239-241`. |
| 3a | cache load | `ChunkCache.load` `160-174`; `wrap` `187-190`/`maybeWrap` `192-198`; `LoadingCache` field `:57`; `ImmediateExecutor` `:152` | **synchronous `LoadingCache`** + immediate executor; `Buffer` refcount CAS `98-144`. |
| 4 | `RandomAccessReader.reBufferAt(pos)` | `RandomAccessReader.java:78-88` (`@NotThreadSafe` at :32) | `bufferHolder.release()` then `rebufferer.rebuffer(pos)`; every `DataInputPlus` read may re-buffer. |
| 5 | BTI trie walk | `Walker.go(long)` `io/tries/Walker.java:94` | holds a `Rebufferer` directly. |
| 6 | index lookup | Big: `BigTableReader.getRowIndexEntry` `big/BigTableReader.java:251`; BTI: `BtiTableReader.getExactPosition` `bti/BtiTableReader.java:235` | |
| 7 | row iterator | `AbstractSSTableIterator` ctor `io/sstable/AbstractSSTableIterator.java:76` | **[PLAN-DELTA]** path is `io/sstable/`, not `io/sstable/format/`. |
| 8 | `SinglePartitionReadCommand.queryStorage` | `SinglePartitionReadCommand.java:556` | |
| 9 | `ReadCommand.executeLocally` | `ReadCommand.java:506` (overload at 500) | materializes lazy chain → disk reads fire. |
| 10 | `StorageProxy.LocalReadRunnable.runMayThrow` | `StorageProxy.java:2721` (class), `:2740` (runMayThrow) | runs on `Stage.READ`. |

`concurrent_reads = 32` at `Config.java:251` (confirmed). No `java.lang.foreign`/`MemorySegment`/`Linker` usage anywhere in `src/java` (confirmed — io_uring FFM would be the first).

### FileHandle.Builder mode dispatch — `FileHandle.java` (the critical selection point)
- `Builder.diskAccessMode` default = `standard` (`:345`).
- **`ioMode()` `450-469`**: `mmap|standard|auto|legacy|mmap_index_only → BUFFERED`; `direct → DIRECT`; **`default: throw new AssertionError("Unhandled diskAccessMode")` at :467**. A new enum value hits this AssertionError unless a case is added.
- **`complete(factory)` `472-536`** rebufferer selection:
  - `length==0` → `EmptyRebufferer` (:488)
  - `mmap` → compressed `CompressedChunkReader.Mmap` (:496) else `MmapRebufferer` (:502)
  - else (standard/direct) → compressed: `direct?CompressedChunkReader.Direct:.Standard` (:510-517); uncompressed → `SimpleChunkReader` (:523)
  - all wrapped by `maybeCached()` `538-543` (→`chunkCache.wrap` if cache enabled & capacity>0).
- Note: uncompressed **`direct`** = `SimpleChunkReader` over an O_DIRECT-opened channel (mode differentiation happens only in `ioMode()`, not in the reader selection).

## 4. IOOptions + per-component access-mode selection

`IOOptions.java`: `fromDatabaseDescriptor()` `28-39` snapshots `defaultDiskAccessMode = getDiskAccessMode()` (`:31`) and `indexDiskAccessMode = getIndexAccessMode()` (`:32`). Immutable fields `41-46`.

Per-component read-handle wiring (where `withDiskAccessMode` is actually called):
- **Data file (read):** `SortedTableReaderLoadingBuilder.java:65` → `withDiskAccessMode(ioOptions.defaultDiskAccessMode)`.
- **Index components (read):** `IndexComponent.fileBuilder` `format/IndexComponent.java:34-35`; BTI row/partition index `bti/BtiTableReaderLoadingBuilder.java:209-210, 224-225` → `withDiskAccessMode(ioOptions.indexDiskAccessMode)`.
- **Per-scan override (compaction reads):** `SSTableReader.openDataReaderInternal` `SSTableReader.java:1448-1467` → `dfile.toBuilder().withDiskAccessMode(diskAccessMode).complete()`; `canReuseDfile` `1469-1474` reuses the buffered `dfile` when `diskAccessMode==direct && !dfile.supportsDirectIO()` (i.e. **direct silently falls back to buffered for uncompressed / unsupported-FS tables**). `supportsDirectIO()` `172-175` = `isDirectIOSupported(file) && compressionMetadata.isPresent()` → effectively compressed-only.
- Compaction-read mode is consumed at `AbstractCompactionStrategy.java:266`, `LeveledCompactionStrategy.java:347/449/501`, `CompactionManager.java:1777/1800`, `CursorCompactor.java:307` via `sstable.getScanner(ranges, DatabaseDescriptor.getCompactionReadDiskAccessMode())`.

**[PLAN-DELTA]** Plan item #4 / §5.6 says "`DataComponent`/`IndexComponent` choose access mode per component." **`DataComponent.java` is WRITE-only** (`buildWriter` `98-139`, gated on `getBackgroundWriteDiskAccessMode()==direct`); it plays **no role** in read access-mode selection. The read data-file mode is chosen in `SortedTableReaderLoadingBuilder:65`.

## 5. StartupChecks — `src/java/org/apache/cassandra/service/StartupChecks.java`

- `DEFAULT_TESTS` list `123-141`; `checkKernelBug1057843` is **first**, `checkDirectIOSupport` at index in `135`.
- **`checkDirectIOSupport` `872-909`**: gates only if `getCompactionReadDiskAccessMode()==direct` (`:886`) OR `getBackgroundWriteDiskAccessMode()==direct` (`:887`); else returns. Calls `findDirectIOUnsupportedLocations(getAllDataFileLocations())` `911-927` → per-dir `FileUtils.isDirectIOSupported` (`:922`); throws `ERR_WRONG_DISK_STATE` (`:900`) listing unsupported dirs (NFS/CIFS/virtual FS). **No kernel-version check here.**
- **`checkKernelBug1057843` `244-302`**: Linux-only (`:258`); inspects `DatabaseDescriptor.getDirectIOWritePaths()` (`:261`) — **WRITE paths only** (populated for `background_write==direct`, `DatabaseDescriptor.java:3490-3492`); filters to `ext4` file stores (`:269-282`); affected kernel range `Range.closedOpen("6.1.64","6.1.66")` LOOSE semver (`:287-288`) vs `FBUtilities.getKernelVersion()` (`:290`); throws `ERR_WRONG_MACHINE_STATE` (`:294`). Suppressible via `IGNORE_KERNEL_BUG_1057843_CHECK` (`:263`, import `:89`).
  - **[PLAN-DELTA / OPEN QUESTION #4 answer]** This check today covers **direct writes only**, not direct *reads*. `compaction_read_disk_access_mode==direct` (O_DIRECT reads) is **already not covered** by the ext4 bug check. So io_uring+O_DIRECT reads would inherit the same gap; if the bug affects reads, `getDirectIOWritePaths()` (or a new read-paths accessor) must be extended.

## 6. cassandra.yaml doc blocks — `conf/cassandra.yaml`
- `disk_access_mode` block `444-459` (documents `auto`/`standard`/`mmap`/`mmap_index_only`; **omits `legacy` and `direct`**; default line commented `# disk_access_mode: mmap_index_only` at `:459`).
- `commitlog_disk_access_mode` block `681-689` (auto/legacy/mmap/direct/standard); active `commitlog_disk_access_mode: legacy` at `:689`.
- `compaction_read_disk_access_mode` block `691-694` (auto/direct only); commented default `:694`.
- `background_write_disk_access_mode` block `696-705`; `direct_write_buffer_size` `707-713`.

## 7. JVM options — **[PLAN-DELTA, important]**
- **`--enable-native-access=ALL-UNNAMED` IS ALREADY PRESENT** at `conf/jvm25-server.options:115` (with explanatory comment `113-114`, added by the JDK25 commit). Plan §3.6 & §5.6 — and the orchestrator's env note — state it is "NOT yet present / must be added." **This is stale/incorrect for the server options file.**
- However it is present **only** in `conf/jvm25-server.options`. It is **absent** from `conf/jvm25-clients.options` (which has only `--add-opens java.base/sun.nio.ch=ALL-UNNAMED` at `:50`) and from **every ant/test build XML** (grep for `enable-native-access` across `*.xml/*.options/*.properties` returns only `jvm25-server.options`). So FFM downcalls under `ant test` / tools / clients still lack the flag — a real remaining TODO the plan should retarget from "add to server options" to "add to test/build harness + clients."
- `--add-opens java.base/sun.nio.ch=ALL-UNNAMED` present in server (`:110`) and clients (`:50`) — enables `NativeLibrary.getfd` reflection (confirmed).

## 8. Enumerated integration seams a new `io_uring` mode MUST touch

1. **Enum** — add `io_uring` to `Config.DiskAccessMode` (`Config.java:1355-1367`).
2. **Data-mode resolution** — `DatabaseDescriptor.java:674-693`: add branch setting `conf.disk_access_mode` + `indexAccessMode` (decide whether `io_uring` is legal top-level, unlike `direct` which throws at :687).
3. **Compaction-read resolution** — `DatabaseDescriptor.java:695-708`: add `else if (io_uring)` branch, else the existing `IllegalArgumentException` (:705) fires.
4. **`FileHandle.ioMode()` `:450-469`** — add a `case io_uring:` returning a new `ChannelProxy.IOMode` (needs a new enum constant in `ChannelProxy.java:47-51`, currently only `BUFFERED`/`DIRECT`), **or the `default:` AssertionError at :467 crashes**.
5. **`FileHandle.complete(factory)` `:472-536`** — add rebufferer/reader selection for `io_uring` (a new `IoUring*ChunkReader` for compressed + uncompressed), plumbed through `maybeCached()`.
6. **`ChannelProxy`** — new `IOMode` + `openOptions()` `69-80` entry; a raw-fd/read path (`read` `169-180`, `getFileDescriptor` `218-221`) or an `IoUringChannelProxy` subtype.
7. **ChunkReader impl(s)** — new class(es) implementing `ChunkReader.readChunk` (`ChunkReader.java:39`), analogous to `SimpleChunkReader`/`CompressedChunkReader.{Standard,Direct}`.
8. **ChunkCache** — optionally `LoadingCache`→`AsyncLoadingCache` for in-flight dedup (`ChunkCache.java:57,150-157,160-174,232-252`); today synchronous + `ImmediateExecutor` (:152).
9. **Per-component read builders** — no code change needed if `io_uring` flows through `ioOptions.defaultDiskAccessMode`/`indexDiskAccessMode` (`SortedTableReaderLoadingBuilder:65`, `IndexComponent:34`, `BtiTableReaderLoadingBuilder:209/224`), but the per-scan override `SSTableReader.canReuseDfile` `1469-1474` needs an `io_uring` analogue to the direct-fallback clause.
10. **StartupCheck** — new `checkIoUringAvailability` added to `DEFAULT_TESTS` (`StartupChecks.java:123-141`), modeled on `checkDirectIOSupport` (`872-909`) + `checkKernelBug1057843` (`244-302`); kernel ≥5.6 via `FBUtilities.getKernelVersion()` + `Range<Semver>`.
11. **Fallback** — on unavailable, `DatabaseDescriptor.setDiskAccessMode(standard)` (`:4059`) **must also `setIndexAccessMode(...)` (`:4071`)** — the two are independent fields; resetting only `disk_access_mode` leaves index files on io_uring. (Plan §5.2 step 5 omits this.)
12. **Kernel-bug check** — per the `Config.java:1364` contract, revisit `checkKernelBug1057843`; note it currently covers **write paths only** (`getDirectIOWritePaths()`), so io_uring O_DIRECT *reads* need a new read-paths hookup if affected.
13. **cassandra.yaml docs** — extend `disk_access_mode` block (`444-459`, which doesn't even document `direct`/`legacy` today) and `compaction_read` block (`691-694`); add any new io_uring tuning knobs.
14. **JVM/test options** — `--enable-native-access` already in `jvm25-server.options:115`; **add to `jvm25-clients.options` and the ant/test build harness** (currently absent everywhere except server options).

## 9. Summary of contradictions vs the draft plan
- **[MAJOR]** `--enable-native-access=ALL-UNNAMED` already present (`jvm25-server.options:115`); plan §3.6/§5.6 and the env note are stale. Real gap is clients + test/build configs.
- **[MODERATE]** `DataComponent` is write-only; read-path data-mode selection lives in `SortedTableReaderLoadingBuilder:65` (plan item #4 mis-attributes it).
- **[MODERATE]** No `index_access_mode` yaml knob exists; `indexAccessMode` is a derived private field (plan item #2 lists it as a knob).
- **[MODERATE]** `checkKernelBug1057843` gates **direct writes only**, not direct reads — answers plan open-question #4 and means io_uring reads aren't auto-covered.
- **[MINOR]** Fallback path must reset both `disk_access_mode` and `indexAccessMode`.
- **[MINOR]** `direct` is illegal as a top-level `disk_access_mode` (throws at `DatabaseDescriptor.java:687`); decide io_uring's stance.
- **[MINOR]** Path nit: `AbstractSSTableIterator.java` is under `io/sstable/`, not `io/sstable/format/`.
- **[CONFIRMED accurate]** enum at 1355-1367; ChunkCache synchronous `LoadingCache`+`ImmediateExecutor` (152/160); CachingRebufferer retry loop (239-241); `concurrent_reads=32` (251); no existing FFM usage; `getfd` at 379/398; `sun.nio.ch` add-opens present.

Key files (absolute): `/home/cscotta/projects/cassandra/src/java/org/apache/cassandra/config/Config.java`, `.../config/DatabaseDescriptor.java`, `.../io/util/FileHandle.java`, `.../io/util/ChannelProxy.java`, `.../io/util/{Rebufferer,BufferManagingRebufferer,MmapRebufferer,SimpleChunkReader,CompressedChunkReader,ChunkReader,RandomAccessReader}.java`, `.../cache/ChunkCache.java`, `.../io/sstable/IOOptions.java`, `.../io/sstable/format/{DataComponent,IndexComponent,SortedTableReaderLoadingBuilder,SSTableReader}.java`, `.../io/sstable/format/bti/BtiTableReaderLoadingBuilder.java`, `.../service/StartupChecks.java`, `/home/cscotta/projects/cassandra/conf/{cassandra.yaml,jvm25-server.options,jvm25-clients.options}`.