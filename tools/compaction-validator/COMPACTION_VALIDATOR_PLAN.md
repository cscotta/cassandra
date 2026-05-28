# CompactionValidator: Implementation Plan

## Context

Cassandra trunk is developing a cursor-based compaction implementation (`CursorCompactionPipeline` / `CursorCompactor`) as a replacement for the legacy iterator-based pipeline (`IteratorCompactionPipeline` / `CompactionIterator`). Before the cursor implementation can be adopted, it must be proven correct (identical output to legacy) and measured for performance improvement.

This plan describes a standalone tool — `tools/compaction-validator/` — that continuously generates random schemas and datasets, runs both compaction implementations over identical input SSTables in parallel, validates byte-for-byte identity of the output LSMs, and displays all of this in a rich terminal UI. It is the primary correctness and performance qualification harness for cursor-based compaction.

---

## 1. High-Level Architecture

```
┌──────────────────────────────────────────────────────────────────────────────┐
│  CompactionValidator (single JVM)                                            │
│                                                                              │
│  Main (picocli)                                                              │
│    └─► RunLoop ──► RunOrchestrator (one run per seed)                        │
│                         │                                                    │
│           ┌─────────────┼──────────────────┐                                │
│           ▼             ▼                  ▼                                 │
│      SchemaGenerator    DataGenerator         ParallelCompactor                      │
│      (Phase 1)     (Phase 2)           (Phase 3)                             │
│           │             │                  │                                 │
│           │             │         ┌────────┴────────┐                       │
│           │             │         ▼                 ▼                        │
│           │             │   [Thread-Legacy]   [Thread-Cursor]               │
│           │             │   DirectCompaction  DirectCompaction               │
│           │             │   Runner(ITERATOR)  Runner(CURSOR)                │
│           │             │         │                 │                        │
│           │             │    IteratorPipeline  CursorPipeline               │
│           │             │    (via PipelineSelector, same package)           │
│           │             │         │                 │                        │
│           │             │   output/legacy/    output/cursor/                │
│           │             │                                                    │
│           └─────────────┴──────────────────► Validator (Phase 4)            │
│                                                   │                          │
│                                        Phase A: xxhash64 per partition       │
│                                        Phase B: cell-by-cell on mismatch    │
│                                                                              │
│  ProgressBus (LinkedBlockingQueue) ──────────────────────────────────────► │
│                                                                              │
│  TUI (Lanterna, dedicated render thread, ~30 fps)                           │
│    ├─ HeaderPanel    (title, run#, seed, phase, ETA)                        │
│    ├─ SchemaPanel    (generated CQL, scrollable)                            │
│    ├─ DataGenPanel   (progress bar, partitions/rows/bytes counters + rates) │
│    ├─ CompactionPanel (side-by-side LSM visualizations, animated)           │
│    ├─ ValidationPanel (dual progress bars + animated SSTable reads)         │
│    └─ FooterPanel    (status messages, last event)                          │
│                                                                              │
│  RunLogger (append-only log file, system stats header)                      │
└──────────────────────────────────────────────────────────────────────────────┘
```

### Threading model

| Thread | Owner | Lifecycle |
|---|---|---|
| Main | `Main.main()` | Bootstraps JVM, starts TUI, drives RunLoop |
| Render | `TuiManager` (Lanterna) | Alive entire process lifetime |
| DataGen-N (N=cores) | `DataGenerator.ExecutorService` | Per data-gen phase |
| Compaction-Legacy | `ParallelCompactor` | Per compaction phase |
| Compaction-Cursor | `ParallelCompactor` | Per compaction phase |
| Validation-A | `Validator` | Per validation phase (hash sweep) |
| Validation-B | `Validator` | Per mismatch (deep-equals, same thread as A) |

---

## 2. Directory Layout

### New files

```
tools/compaction-validator/
├── build.xml                              # Ant subproject (imports ../../build.xml)
├── src/
│   └── org/apache/cassandra/
│       ├── db/compaction/                 # Bridge classes in production package
│       │   ├── PipelineSelector.java      # Instantiates pipeline by backend enum
│       │   └── DirectCompactionRunner.java# Replicates runMayThrow() w/ selectable backend
│       └── tools/compactionvalidator/     # Tool-specific classes
│           ├── Main.java                  # picocli root command + JVM bootstrap
│           ├── RunLoop.java               # Continuous-run driver, seed management
│           ├── RunOrchestrator.java       # Single run: schema→data→compact→validate→cleanup
│           ├── RunSeed.java               # Seed envelope (root seed + phase seeds)
│           ├── RunResult.java             # Result record (success/failure + stats)
│           ├── schema/
│           │   ├── SchemaGenerator.java       # Deterministic schema generator
│           │   ├── SchemaGeneratorConfig.java # Tuning knobs (column counts, type weights)
│           │   ├── GeneratedSchema.java   # Output: CQL string + TableMetadata
│           │   └── TypeRegistry.java      # Full CQL type catalog (extends Harry's)
│           ├── data/
│           │   ├── DataGenerator.java         # Parallel SSTable writer orchestrator
│           │   ├── DataGeneratorConfig.java   # target bytes, thread count, size bounds
│           │   ├── PartitionWriter.java   # Writes one partition's worth of rows/tombstones
│           │   ├── RowGenerator.java      # Generates rows/cells/tombstones from seed
│           │   └── DataGenStats.java      # Counters: partitions, rows, bytes, rates
│           ├── compaction/
│           │   ├── ParallelCompactor.java # Launches and monitors two compaction threads
│           │   ├── CompactionDriver.java  # Drives one CFS through background compaction tasks
│           │   ├── CompactionStats.java   # Counters: partitions, rows, bytes, rates
│           │   └── SstableSetManager.java # Creates working dirs, copies source set, cleanup
│           ├── validation/
│           │   ├── Validator.java         # Two-phase validation orchestrator
│           │   ├── PartitionHasher.java   # Phase A: xxhash64 per UnfilteredRowIterator
│           │   ├── PartitionComparator.java# Phase B: cell-by-cell deep-equals
│           │   ├── MismatchReport.java    # Structured mismatch detail for display + log
│           │   └── ValidationStats.java   # Counters: partitions, rows, bytes, mismatches
│           ├── tui/
│           │   ├── TuiManager.java        # Lanterna screen lifecycle, render loop
│           │   ├── TuiPanel.java          # Base panel interface
│           │   ├── HeaderPanel.java       # Title, run#, seed, phase, ETA
│           │   ├── SchemaPanel.java       # Scrollable CQL display
│           │   ├── DataGenPanel.java      # Progress bar, counters, rates
│           │   ├── CompactionPanel.java   # Side-by-side LSM visualizer (animated)
│           │   ├── LsmTreeWidget.java     # Unicode box-art SSTable level display
│           │   ├── ValidationPanel.java   # Dual progress + SSTable read animation
│           │   ├── SummaryPanel.java      # Completion stats + perf multiplier
│           │   ├── ProgressBus.java       # LinkedBlockingQueue<TuiEvent>
│           │   └── TuiEvent.java          # Tagged event union (sealed-interface style)
│           ├── logging/
│           │   ├── RunLogger.java         # Append-only log writer
│           │   └── SystemInfo.java        # Core count, memory, OS version at startup
│           └── util/
│               ├── SeedUtil.java          # Seed splitting, pretty-printing
│               ├── ByteUtil.java          # Human-readable sizes (10.3 GiB)
│               └── RateTracker.java       # Exponential moving average for rates
└── test/
    └── unit/
        └── org/apache/cassandra/tools/compactionvalidator/
            ├── SchemaGeneratorTest.java
            ├── DataGeneratorTest.java
            ├── ValidatorTest.java
            └── PartitionHasherTest.java
```

### Modified files

| File | Change | Anchor lines |
|---|---|---|
| `build.xml` | Add `<import>` for subproject | Near line 2352 (after sstableloader import) |
| `build.xml` | Append `compaction-validator-jar` to `_artifacts-init` depends | Line 968 |
| `build.xml` | Append `compaction-validator-build-test` to `build-test` depends | Line 1205 |
| `build.xml` | Add `${cv.build.classes}` to test classpath path elements | Lines 1224, 1265, 1398 |
| `build.xml` | Add `${cv.test.classes}` to test classpath path elements | Lines 1253, 1269, 1403 |
| `build.xml` | Add Eclipse `<classpathentry>` for src and test | Lines 2234, 2237 |
| `.build/cassandra-deps-maven-pom.xml` | Add `com.googlecode.lanterna:lanterna:3.1.2` dependency | After picocli entry (~line 127) |
| `.build/cassandra-build-maven-pom.xml` | Add lanterna version in `<dependencyManagement>` | In the BOM section |
| `tools/bin/compaction-validator` | New launcher shell script | New file |

---

## 3. Subsystem Designs

### 3.1 Pipeline Adapter (in `org.apache.cassandra.db.compaction`)

**Purpose:** Expose package-private pipeline constructors to the tool without modifying any production compaction class.

**File:** `tools/compaction-validator/src/org/apache/cassandra/db/compaction/PipelineSelector.java`

```java
package org.apache.cassandra.db.compaction;

public final class PipelineSelector {
    public enum Backend { ITERATOR, CURSOR }

    /** Create a pipeline for the given backend, bypassing the global cursorCompactionEnabled flag. */
    public static AbstractCompactionPipeline create(
            Backend backend,
            CompactionTask task,
            OperationType type,
            AbstractCompactionStrategy.ScannerList scanners,
            AbstractCompactionController controller,
            long nowInSec,
            TimeUUID compactionId) {
        return backend == Backend.CURSOR
            ? new CursorCompactionPipeline(task, type, scanners, controller, nowInSec, compactionId)
            : new IteratorCompactionPipeline(task, type, scanners, controller, nowInSec, compactionId);
    }
}
```

**File:** `tools/compaction-validator/src/org/apache/cassandra/db/compaction/DirectCompactionRunner.java`

This class is the heart of the tool's compaction execution. It replicates the critical path from `CompactionTask.runMayThrow()` (lines 170–380 of `CompactionTask.java`) but with a selectable pipeline backend and a progress callback. Because it lives in `org.apache.cassandra.db.compaction`, it can access all `protected` fields of `AbstractCompactionTask` and `CompactionTask`.

```java
package org.apache.cassandra.db.compaction;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.function.LongConsumer;

import org.apache.cassandra.db.ColumnFamilyStore;
import org.apache.cassandra.db.compaction.writers.CompactionAwareWriter;
import org.apache.cassandra.db.lifecycle.LifecycleTransaction;
import org.apache.cassandra.io.sstable.format.SSTableReader;
import org.apache.cassandra.utils.FBUtilities;
import org.apache.cassandra.utils.TimeUUID;

/** Executes a single compaction task with an explicitly-chosen backend pipeline. */
public class DirectCompactionRunner {

    public interface ProgressCallback {
        /** Called every ~1MB of scanned bytes. */
        void onProgress(long bytesScanned, long estimatedTotal, long partitionsWritten, long rowsWritten);
        /** Called when an SSTable is completed (for TUI LSM animation). */
        void onSStableEmitted(SSTableReader sstable);
    }

    private final PipelineSelector.Backend backend;
    private final ProgressCallback callback;

    public DirectCompactionRunner(PipelineSelector.Backend backend, ProgressCallback callback) {
        this.backend = backend;
        this.callback = callback;
    }

    /**
     * Run a compaction task.
     *
     * @param task       A CompactionTask already configured with its transaction (from
     *                   strategy.getNextBackgroundTasks or strategy.getMaximalTasks).
     * @param tracker    Pass ActiveCompactionsTracker.NOOP for offline use.
     */
    public Collection<SSTableReader> run(CompactionTask task, ActiveCompactionsTracker tracker) throws Exception {
        // Access package-visible fields on CompactionTask / AbstractCompactionTask:
        //   task.cfs, task.transaction, task.compactionType, task.gcBeforeSeconds, task.keepOriginals

        ColumnFamilyStore cfs = task.cfs;
        Set<SSTableReader> inputSSTables = new HashSet<>(task.transaction.originals());

        // 1. Identify fully-expired SSTables (same as CompactionTask.runMayThrow, line ~200)
        try (CompactionController controller = task.getCompactionController(inputSSTables)) {
            Set<SSTableReader> actuallyCompact = new HashSet<>(inputSSTables);
            Set<SSTableReader> fullyExpired = controller.getFullyExpiredSSTables();
            fullyExpired.forEach(task.transaction::obsolete);
            actuallyCompact.removeAll(fullyExpired);

            long nowInSec = FBUtilities.nowInSeconds();
            TimeUUID taskId = task.transaction.opId();

            // 2. Open scanners
            CompactionStrategyManager strategy = cfs.getCompactionStrategyManager();
            try (AbstractCompactionStrategy.ScannerList scanners = strategy.getScanners(actuallyCompact, null);
                 // 3. Create pipeline with explicitly chosen backend
                 AbstractCompactionPipeline pipeline = PipelineSelector.create(
                     backend, task, task.compactionType, scanners, controller, nowInSec, taskId)) {

                tracker.beginCompaction(pipeline);
                // 4. Open writer via existing CompactionTask.getCompactionAwareWriter
                try (AutoCloseable writer = task.getCompactionAwareWriter(actuallyCompact, pipeline)) {
                    long lastBytesScanned = 0;
                    long estimatedKeys = pipeline.estimatedKeys();
                    while (pipeline.processNextPartitionKey()) {
                        long bytesScanned = pipeline.getTotalBytesScanned();
                        if (bytesScanned - lastBytesScanned > 1024 * 1024) {
                            callback.onProgress(bytesScanned, scanners.getTotalCompressedSize(),
                                                pipeline.getTotalKeysWritten(),
                                                pipeline.getTotalSourceCQLRows());
                            lastBytesScanned = bytesScanned;
                        }
                    }
                    // 5. Finish (point of no return)
                    return task.finish(pipeline); // protected, accessible same package
                } finally {
                    tracker.finishCompaction(pipeline);
                }
            }
        }
    }
}
```

**Notes on field access:** `AbstractCompactionTask` (the direct parent of `CompactionTask`) declares `cfs`, `transaction`, `isUserDefined`, `compactionType` as `protected` (lines 34-37 of `AbstractCompactionTask.java`). `CompactionTask` declares `gcBeforeSeconds` and `keepOriginals` as `protected` (lines 86-87). All are accessible from `DirectCompactionRunner` because it is in the same package.

The method `getCompactionController(Set<SSTableReader>)` is `protected final` on `CompactionTask` (line 561) — accessible same-package. The method `finish(AbstractCompactionPipeline)` is `protected` (line 386) — accessible same-package. The method `getCompactionAwareWriter(Set<SSTableReader>, AbstractCompactionPipeline)` is `protected` (line 394) — accessible same-package.

---

### 3.2 Bootstrap & Offline CFS Provisioning

**Init sequence** (modeled on `CompactionStress` static block + `ClusterMetadataService.initializeForTools`):

```java
// In Main.java static block or explicit init() method
DatabaseDescriptor.daemonInitialization();
CommitLog.instance.start();
ClusterMetadataService.initializeForTools(true);  // true = enable schema mutations
Keyspace.setInitialized();
// Force BIG format (required for cursor compaction compatibility)
DatabaseDescriptor.setSelectedSSTableFormat(SSTableFormat.Type.BIG.info);
// Force Murmur3 (required for CursorCompactor.isSupported)
DatabaseDescriptor.setPartitionerUnsafe(Murmur3Partitioner.instance);
```

**CFS creation** — delegate to `StressCQLSSTableWriter.Builder.createOfflineTable(String schema, List<File> dirs)` at `tools/stress/src/org/apache/cassandra/io/sstable/StressCQLSSTableWriter.java:624`. This handles keyspace registration, schema transformation submission, deterministic TableId, and `ColumnFamilyStore.createColumnFamilyStore(...)`.

We call it twice per run (once per output dir): once for the legacy CFS and once for the cursor CFS. Both use the **same** CQL schema string but different directory lists.

**Input CFS** (for data generation): also uses `createOfflineTable` pointing at the source dir.

---

### 3.3 Schema Generator

**Files:**
- `tools/compaction-validator/src/org/apache/cassandra/tools/compactionvalidator/schema/SchemaGenerator.java`
- `tools/compaction-validator/src/org/apache/cassandra/tools/compactionvalidator/schema/TypeRegistry.java`
- `tools/compaction-validator/src/org/apache/cassandra/tools/compactionvalidator/schema/GeneratedSchema.java`

**Harry integration:**
Harry's `ColumnSpec` (at `test/harry/main/org/apache/cassandra/harry/ColumnSpec.java:522`) defines `TYPES` as a list of supported column types. We do NOT modify Harry; instead, `TypeRegistry.java` assembles the full type set independently, drawing from `AbstractTypeGenerators.java` (`test/unit/org/apache/cassandra/utils/AbstractTypeGenerators.java:147,184`):

**Full cursor-compatible primitive types to generate:**
`ByteType`, `ShortType`, `Int32Type`, `LongType`, `IntegerType` (varint), `FloatType`, `DoubleType`, `DecimalType`, `BooleanType`, `AsciiType`, `UTF8Type`, `BytesType`, `TimestampType`, `SimpleDateType`, `TimeType`, `DurationType`, `UUIDType`, `TimeUUIDType`, `InetAddressType`.

**Full cursor-compatible complex types to generate:**
`SetType<T>`, `ListType<T>`, `MapType<K,V>`, `TupleType(T1,T2,...)`, `UserType` (UDT), `FrozenType` variants of the above.

**Excluded types** (cursor-incompatible or special):
- `CounterColumnType` — requires counter CFS semantics (excluded by `CursorCompactor.unsupportedMetadata` check or schema constraints).
- `VectorType` — check `CursorCompactor.unsupportedMetadata` before including; add only if allowed.
- Secondary indexes — never generated (would violate `isSupported`).

**Schema structure per run (all controlled by seed):**

```
keyspace: cvtest_<hex(seed)>
table:    t
strategy: UnifiedCompactionStrategy with default scaling parameters (no explicit scaling_parameters — uses UCS defaults)
  - target_sstable_size: random in [64MiB, 512MiB]
  - base_shard_count: random in [1, 4]
  - min_sstable_size: 10MiB
compression: LZ4 with chunk_length_in_kb = random in [4, 64, 256, 512, 1024] (≤ 1MB limit)
tombstone_threshold: 1.0 (effectively disabling garbage-skipping; keeps tombstone option NONE)

partition key:  1-3 columns, types from {Int32, Long, UUID, ASCII(16), UTF8(16)}
clustering key: 0-3 columns, types from primitives, mix of ASC/DESC
static columns: 0-3 columns, any legal type
regular columns: 3-12 columns, any legal type including complex
```

**Key invariants enforced:**
- No secondary indexes ever (violates cursor `isSupported`).
- Keyspace name never `SchemaConstants.ACCORD_KEYSPACE_NAME`.
- Partitioner always Murmur3 (set globally at boot).
- SSTable format always BIG (set globally at boot).
- Compaction params never include `tombstone_compaction_interval` or `unchecked_tombstone_compaction` that would trigger non-NONE tombstone option.
- No `CounterColumnType` columns.

**`GeneratedSchema`** output:

```java
public record GeneratedSchema(
    String cql,                  // Full CREATE TABLE + CREATE TYPE statements
    TableMetadata metadata,      // For reference during data generation
    String keyspaceName,
    String tableName
) {}
```

---

### 3.4 Data Generator + Parallel SSTable Writers

**Files:**
- `tools/compaction-validator/src/org/apache/cassandra/tools/compactionvalidator/datagen/DataGenerator.java`
- `tools/compaction-validator/src/org/apache/cassandra/tools/compactionvalidator/datagen/PartitionWriter.java`
- `tools/compaction-validator/src/org/apache/cassandra/tools/compactionvalidator/datagen/RowGenerator.java`
- `tools/compaction-validator/src/org/apache/cassandra/tools/compactionvalidator/datagen/DataGenStats.java`

**Writer:** Use `CQLSSTableWriter` (`src/java/org/apache/cassandra/io/sstable/CQLSSTableWriter.java:137`) — the **main-source-tree** public API. Do NOT use `HarrySSTableWriter` (test source; would create a production tool jar that depends on test code — see R4). `CQLSSTableWriter.Builder` accepts the generated CQL schema string directly and handles `clientInitialization` in its static block, but since we call `daemonInitialization()` first in `bootstrapJvm()`, the static block's `clientInitialization()` call will be a no-op (DD is already initialized). Verify this at Milestone 1 test time.

**Approach:**

`DataGenerator` uses an `ExecutorService` with `availableProcessors()` threads. Each thread gets its own `CQLSSTableWriter` instance (thread-safety: each writer owns its own file handles and `AbstractSSTableSimpleWriter`). Each thread is given a contiguous sub-range of the partition key space.

Each thread's writer is configured with a random `maxSSTableSizeInMiB` drawn from `[1, 1024]` (1 MiB to 1 GiB) using that thread's sub-seed.

**Data variety per partition (all driven by seed):**

| Element | Probability | Notes |
|---|---|---|
| Static row | 30% | Only if schema has static columns |
| Regular rows | 1–100 rows | Count drawn from exponential distribution |
| Point delete | 10% per row | `DELETE FROM t WHERE pk=? AND ck=?` with timestamp |
| Range delete | 5% per partition | `DELETE FROM t WHERE pk=? AND ck >= ? AND ck < ?` |
| TTL per column | 15% per column | Random TTL in [60, 86400] seconds |
| Null value | 10% per non-PK column | `null` bound variable |
| Empty collection | 10% per collection col | Empty `{}` or `[]` |
| Complex collection | varies | Live + deleted elements in same collection |
| Max cell size | uniform in [1, 1MB] | For variable-length types |

**Progress tracking:** `DataGenStats` holds `AtomicLong` counters for `bytesWritten`, `partitionsWritten`, `rowsWritten`. Updated every SSTable flush via `CQLSSTableWriter.Builder.withSSTableProducedListener(...)` (equivalent hook in `CQLSSTableWriter`). The ProgressBus receives `DataGenProgress` events for TUI rendering.

**Completion condition:** Run until `totalBytesWritten >= targetBytes` (default 10 GiB). The `WorkManager` / target-driven stop is modeled after `CompactionStress.DataWriter.run()` at `tools/stress/src/org/apache/cassandra/stress/CompactionStress.java:~318`.

**SSTable loading into CFS after generation:** Use `CompactionStress.initCf`-style pattern (`SSTableReader.openNoValidation` + `cfs.addSSTables(sstables)` + `cfs.disableAutoCompaction()`) — see `CompactionStress.java:122-167`.

---

### 3.5 Parallel Compaction Driver

**Files:**
- `tools/compaction-validator/src/org/apache/cassandra/tools/compactionvalidator/compaction/ParallelCompactor.java`
- `tools/compaction-validator/src/org/apache/cassandra/tools/compactionvalidator/compaction/CompactionDriver.java`
- `tools/compaction-validator/src/org/apache/cassandra/tools/compactionvalidator/compaction/SstableSetManager.java`

**Setup:**
`SstableSetManager` creates the working directory structure:
```
<working-dir>/<run-seed>/
    source/       ← generated SSTables (read-only after DataGenerator completes)
    output-legacy/ ← hard-linked copies of source/ for legacy compaction
    output-cursor/ ← hard-linked copies of source/ for cursor compaction
```
Hard links (`Files.createLink`) avoid copying 10 GiB while giving each CFS its own independent namespace.

**Parallel execution:**

`ParallelCompactor` creates two `CompactionDriver` instances and submits them to a 2-thread `ExecutorService`. Both start simultaneously. They share no state except the read-only source SSTables.

```java
public class ParallelCompactor {
    public record Results(
        CompactionStats legacyStats,
        CompactionStats cursorStats,
        Duration legacyDuration,
        Duration cursorDuration
    ) {}

    public Results run(
        ColumnFamilyStore legacyCfs,
        ColumnFamilyStore cursorCfs,
        ProgressBus bus
    ) throws Exception { ... }
}
```

**`CompactionDriver`** drives one CFS through incremental background compaction until `getEstimatedRemainingTasks() == 0`:

```java
public class CompactionDriver {
    private final ColumnFamilyStore cfs;
    private final PipelineSelector.Backend backend;
    private final ProgressBus bus;

    public CompactionStats run() throws Exception {
        CompactionStrategyManager strategy = cfs.getCompactionStrategyManager();
        long gcBefore = FBUtilities.nowInSeconds();

        while (true) {
            Collection<AbstractCompactionTask> tasks =
                strategy.getNextBackgroundTasks(gcBefore);
            if (tasks.isEmpty()) break;

            for (AbstractCompactionTask task : tasks) {
                if (!(task instanceof CompactionTask)) {
                    // Fallback for non-CompactionTask subtypes (e.g. SingleSSTableLCSTask)
                    task.execute(ActiveCompactionsTracker.NOOP);
                    continue;
                }
                CompactionTask ct = (CompactionTask) task;
                DirectCompactionRunner runner = new DirectCompactionRunner(backend, makeCallback(bus));
                Collection<SSTableReader> newSSTables = runner.run(ct, ActiveCompactionsTracker.NOOP);
                // Emit SSTable-emitted events for TUI animation
                newSSTables.forEach(s -> bus.push(TuiEvent.sstableEmitted(backend, s)));
            }
        }
        return stats;
    }
}
```

**TUI LSM animation feed:** Every time `DirectCompactionRunner.ProgressCallback.onSStableEmitted()` fires, a `TuiEvent.SstableEmitted(backend, sstable)` is pushed to `ProgressBus`. The `LsmTreeWidget` consumes these events to animate sstable movements between levels/buckets.

---

### 3.6 Two-Phase Validator

**Files:**
- `tools/compaction-validator/src/org/apache/cassandra/tools/compactionvalidator/validation/Validator.java`
- `tools/compaction-validator/src/org/apache/cassandra/tools/compactionvalidator/validation/PartitionHasher.java`
- `tools/compaction-validator/src/org/apache/cassandra/tools/compactionvalidator/validation/PartitionComparator.java`
- `tools/compaction-validator/src/org/apache/cassandra/tools/compactionvalidator/validation/MismatchReport.java`

**Phase A: Partition hash sweep**

Reads both output LSMs in parallel (two threads), one partition at a time, computing an xxhash64 digest over each partition's serialized `UnfilteredRowIterator` stream. Compares digests.

Partition iteration API:
```java
// Open all output SSTables
List<SSTableReader> sstables = openOutputSSTables(outputDir, cfs);
// Build merged scanner over all of them (handles multi-shard UCS output)
List<ISSTableScanner> scanners = sstables.stream()
    .map(SSTableReader::getScanner)
    .collect(toList());
UnfilteredPartitionIterator merged =
    UnfilteredPartitionIterators.merge(scanners, UnfilteredPartitionIterators.MergeListener.NOOP);
```

xxhash64 hashing:
```java
// For each partition:
UnfilteredRowIterator partition = merged.next();
long hash = hashPartition(partition);  // Uses net.jpountz.xxhash from lib/ (already present as part of LZ4 lib)

// hashPartition implementation:
long hash = 0;
hash = XxHash64.hash(partitionKey.getKey(), 0, seed);
// Iterate rows and range tombstones, serialize each Unfiltered to a ByteBuffer,
// fold into rolling hash:
while (partition.hasNext()) {
    Unfiltered u = partition.next();
    ByteBuffer serialized = UnfilteredSerializer.serializer.serialize(u, ...);
    hash ^= XxHash64.hash(serialized, seed);
}
```

**Phase B: Cell-by-cell deep-equals on mismatch**

When a partition's hashes differ, re-open that specific partition from both outputs (using `SSTableReader.getScanner(bounds)` where bounds = that partition key's range) and compare `Unfiltered` by `Unfiltered`, `Cell` by `Cell`, checking column, timestamp, TTL, value bytes, deletion info.

```java
public MismatchReport deepCompare(
    DecoratedKey partitionKey,
    List<SSTableReader> legacySSTables,
    List<SSTableReader> cursorSSTables
) { ... }
```

`MismatchReport` records:
```java
public record MismatchReport(
    DecoratedKey partitionKey,
    @Nullable Clustering<?> clusteringOfFirstDiff,
    @Nullable ColumnMetadata columnOfFirstDiff,
    String legacyDescription,
    String cursorDescription,
    int partitionsChecked,
    int partitionMismatches
) {}
```

---

### 3.7 Lanterna TUI

**Files:**
- `tools/compaction-validator/src/org/apache/cassandra/tools/compactionvalidator/tui/TuiManager.java`
- `tools/compaction-validator/src/org/apache/cassandra/tools/compactionvalidator/tui/ProgressBus.java`
- `tools/compaction-validator/src/org/apache/cassandra/tools/compactionvalidator/tui/TuiEvent.java`
- + one `*Panel.java` per panel listed in Section 2

**Lanterna version and dependency:** `com.googlecode.lanterna:lanterna:3.1.2`. Add to `.build/cassandra-deps-maven-pom.xml`:
```xml
<dependency>
  <groupId>com.googlecode.lanterna</groupId>
  <artifactId>lanterna</artifactId>
</dependency>
```
Add to `.build/cassandra-build-maven-pom.xml` `<dependencyManagement>`:
```xml
<dependency>
  <groupId>com.googlecode.lanterna</groupId>
  <artifactId>lanterna</artifactId>
  <version>3.1.2</version>
</dependency>
```
Run `ant resolver-retrieve-build` to download `lib/lanterna-3.1.2.jar`.

**TUI layout (Lanterna `Panel` hierarchy):**

```
DefaultScreen (UNIX terminal, auto-detected size)
└── mainPanel (LinearLayout VERTICAL)
    ├── headerPanel    (1 row, LinearLayout HORIZONTAL)
    │   Title | Run #N | Seed: 0x... | Phase: [COMPACTING] | ETA: 00:03:42
    ├── schemaPanel    (scrollable, 8 rows)
    │   CREATE TABLE cvtest_... (CQL, syntax-colored)
    ├── dataGenPanel   (4 rows)
    │   [████████████░░░░░░░░░░░░] 4.7/10.0 GiB  |  1.2M partitions  |  48.3M rows
    │   Rate: 312 MiB/s  |  84K partitions/s  |  2.3M rows/s
    ├── compactionPanel (10 rows, side-by-side)
    │   ┌─── LEGACY ITERATOR ────────┐  ┌─── CURSOR ─────────────────┐
    │   │  L0 [■■■■ 128M][■■ 64M]   │  │  L0 [■■■■ 128M][■■ 64M]   │
    │   │  L1 [■■■■■■■ 512M]        │  │  L1 [■■■■■■■ 512M]        │
    │   │  L2 [■■■ 2.1G]  ◄──── ✦  │  │  L2 [■■■ 2.1G]  ◄──── ✦  │
    │   │  Throughput: 245 MiB/s    │  │  Throughput: 289 MiB/s    │
    │   └────────────────────────────┘  └────────────────────────────┘
    ├── validationPanel (4 rows)
    │   LEGACY  [██████████████░░░░░░] 78% | 312K partitions | ████ reading [na-1-big-Data.db]
    │   CURSOR  [██████████████░░░░░░] 78% | 312K partitions | ████ reading [na-1-big-Data.db]
    └── footerPanel (1 row, status messages)
        ✓ Run 47 complete | 7.3s until next run | All 312,481 partitions matched
```

**Render loop:** Lanterna's `screen.startScreen()` + periodic `screen.refresh()` driven by a dedicated thread at 30 fps. `TuiManager.renderFrame()` drains `ProgressBus` to apply state updates before each frame.

**Animation elements:**
- Compaction: ASCII "✦" moves along lines between SSTable boxes when a compaction is active. SSTable boxes change color (yellow = being read, green = newly written). Use `TextColor.ANSI` or `TextColor.RGB` for 24-bit color if terminal supports it.
- Validation: A sliding "cursor" character (e.g. `▶`) sweeps across each SSTable filename label as it is being read.
- All animation state is tracked in panel-local fields, updated by `ProgressBus` events.

**`--no-ui` flag:** When set, `TuiManager` is replaced by `PlainTextReporter`, which writes progress lines to stdout/stderr. This allows use in CI environments.

**TuiEvent sealed-interface style** (Java 16+ permits, but for JDK 11 compatibility use tagged classes):
```java
public abstract class TuiEvent {
    public enum Tag { DATA_GEN_PROGRESS, COMPACTION_PROGRESS, SSTABLE_EMITTED, VALIDATION_PROGRESS, RUN_COMPLETE, RUN_FAILED }
    public final Tag tag;
    // Subclass per event type
    public static class DataGenProgress extends TuiEvent { ... }
    public static class SstableEmitted extends TuiEvent {
        public final PipelineSelector.Backend backend;
        public final String sstableFilename;
        public final long sizeBytes;
        public final int level;
    }
    // etc.
}
```

---

### 3.8 Run Logger + Reproducer

**File:** `tools/compaction-validator/src/org/apache/cassandra/tools/compactionvalidator/logging/RunLogger.java`

The log file is opened in `APPEND` mode on startup and never closed until process exit. Header written once per process launch:

```
=== CompactionValidator started 2026-05-24T14:32:01Z ===
Host: hostname  OS: Linux 6.18.5  Cores: 32  RAM: 128GiB  JVM: OpenJDK 21
Tool version: Cassandra 7.0-SNAPSHOT (git: abc123)
Working dir: /tmp/cassandra-compaction-validator
---
```

Per-run entry (appended on completion):

```
[2026-05-24T14:32:09Z] Run #47 seed=0xDEADBEEF12345678
  Schema:  cvtest_deadbeef.t  (5 PK, 2 CK, 3 static, 8 regular cols)
  UCS:     scaling_parameters=T4:T8:N  target_sstable_size=256MiB  base_shard_count=2
  DataGen: 10.3 GiB  |  1.4M partitions  |  52M rows  |  310 MiB/s avg
  Legacy:  18.4s  |  290 MiB/s  |  1.4M partitions/s
  Cursor:  15.7s  |  340 MiB/s  |  1.6M partitions/s  |  Speedup: 1.17x
  Valid:   PASS  |  1.4M partitions matched  |  52M rows matched
[2026-05-24T14:33:01Z] Run #48 seed=0xCAFEBABE98765432  FAIL
  ...
  Valid:   FAIL  |  Partition 0x8a3f... mismatch at ck=[3, "hello"] col=data_col
  Preserved: /tmp/cassandra-compaction-validator/0xcafebare98765432/
  Re-run:  compaction-validator --seed 0xCAFEBABE98765432 --once --no-cleanup
```

**On validation failure:**
- All three directories (`source/`, `output-legacy/`, `output-cursor/`) under `<working-dir>/<seed>/` are preserved.
- The MismatchReport is printed prominently in the TUI (red background, blinking seed display).
- The tool does NOT auto-restart; it waits for user input (any key) before offering options (Q=quit, R=retry with same seed, N=next seed).

---

### 3.9 CLI / picocli Command Tree

**File:** `tools/compaction-validator/src/org/apache/cassandra/tools/compactionvalidator/Main.java`

```java
@Command(
    name = "compaction-validator",
    description = "Validate correctness and measure performance of cursor-based compaction vs. legacy",
    mixinStandardHelpOptions = true,
    subcommands = { CommandLine.HelpCommand.class }
)
public class Main implements Runnable {
    @Option(names = {"--seed", "-s"}, description = "Root RNG seed (hex or decimal). Default: random.")
    Long seed = null;

    @Option(names = {"--target-bytes", "-b"}, description = "SSTable data to generate per run (e.g. 10G, 500M). Default: 10G.")
    String targetBytes = "10G";

    @Option(names = {"--working-dir", "-w"}, description = "Working directory for SSTables. Default: ${java.io.tmpdir}/cassandra-compaction-validator")
    File workingDir = null;  // resolved in run()

    @Option(names = {"--log-file", "-l"}, description = "Append-only log file path. Default: ./compaction-validator.log")
    File logFile = new File("compaction-validator.log");

    @Option(names = {"--once"}, description = "Run once then exit (default: continuous loop).")
    boolean runOnce = false;

    @Option(names = {"--no-cleanup"}, description = "Preserve SSTables even on successful run.")
    boolean noCleanup = false;

    @Option(names = {"--no-ui"}, description = "Plain stdout output (no Lanterna TUI). For CI/piped use.")
    boolean noUi = false;

    @Option(names = {"--threads"}, description = "Data generation thread count. Default: availableProcessors().")
    int threads = Runtime.getRuntime().availableProcessors();

    public static void main(String[] args) {
        bootstrapJvm();
        new CommandLine(new Main()).execute(args);
    }

    public void run() { ... }  // constructs RunLoop, TuiManager, RunLogger, starts everything

    private static void bootstrapJvm() {
        DatabaseDescriptor.daemonInitialization();
        CommitLog.instance.start();
        ClusterMetadataService.initializeForTools(true);
        Keyspace.setInitialized();
        DatabaseDescriptor.setSelectedSSTableFormat(SSTableFormat.Type.BIG.info);
        DatabaseDescriptor.setPartitionerUnsafe(Murmur3Partitioner.instance);
    }
}
```

**Launcher script** `tools/bin/compaction-validator`:
```bash
#!/bin/sh
. "$(dirname "$0")/cassandra.in.sh"
exec "$JAVA" $JVM_OPTS \
  -Dcassandra.storagedir="$(dirname "$0")/../data" \
  -Dlogback.configurationFile=logback-tools.xml \
  -cp "$CLASSPATH:$CASSANDRA_HOME/build/tools/lib/compaction-validator.jar:$CASSANDRA_HOME/lib/lanterna-3.1.2.jar" \
  org.apache.cassandra.tools.compactionvalidator.Main "$@"
```
Note: `cassandra.in.sh` already puts `$CASSANDRA_HOME/lib/*.jar` on `$CLASSPATH`, so the explicit Lanterna path is redundant if it lands in `lib/` — include as belt-and-suspenders.

---

### 3.10 Continuous-Run Loop and Shutdown Hooks

**File:** `tools/compaction-validator/src/org/apache/cassandra/tools/compactionvalidator/RunLoop.java`

```java
public class RunLoop {
    private volatile boolean running = true;

    public RunLoop(Main config, TuiManager tui, RunLogger logger) { ... }

    public void run() throws Exception {
        Runtime.getRuntime().addShutdownHook(new Thread(this::shutdown));

        long seed = config.seed != null ? config.seed : new SecureRandom().nextLong();
        int runNumber = 0;

        while (running) {
            runNumber++;
            RunOrchestrator orchestrator = new RunOrchestrator(seed, config, tui, logger, runNumber);
            RunResult result = orchestrator.execute();

            if (result.isSuccess() && !config.noCleanup) {
                result.cleanup();
            }

            logger.appendRunEntry(result);
            tui.showSummary(result, 7_000);  // 7-second summary display

            if (result.isFailure()) {
                tui.waitForUserInput();  // blocks until keypress
                if (config.runOnce) break;
            }
            if (config.runOnce) break;

            seed = nextSeed(seed);  // deterministic seed progression: seed = hash(seed ^ runNumber)
        }
    }

    private void shutdown() {
        running = false;
        // Force cleanup of any in-progress compactions
        CompactionManager.instance.forceShutdown();
    }
}
```

---

## 4. Determinism Contract

A single 64-bit root seed flows through all random decisions:

```
rootSeed
├── schemaForge.seed  = rootSeed ^ 0x1111111111111111L
│     Drives: number of PK/CK/static/regular columns, type selection per column,
│             ASC/DESC clustering direction, compression chunk size, UCS parameters
├── dataForge.seed    = rootSeed ^ 0x2222222222222222L
│     Split per thread: threadSeed[i] = dataForge.seed ^ (i * 0x1000000000000000L)
│     Each thread drives: partition key values, row counts, cell values, delete/TTL decisions,
│                         SSTable max size selection
└── validator.seed    = rootSeed ^ 0x3333333333333333L (unused currently; reserved)
```

All random generators use `java.util.Random(seed)`, which is deterministic across JVM invocations. The seed is printed in the UI header and log at the start of every run, and `--seed` CLI flag restores it exactly.

The `keyspace` name is derived as `cvtest_` + `Long.toHexString(rootSeed)`, ensuring the schema metadata registered in `Schema.instance` is unique per run and cleanly replaceable.

---

## 5. Cursor Compatibility Enforcement

The schema generator must never produce a schema that causes `CursorCompactor.isSupported()` to return `false`. The relevant checks from `CursorCompactor.java:118-156`:

| Check | Our constraint | Enforced where |
|---|---|---|
| `!(format instanceof BigFormat)` | Set `DatabaseDescriptor.setSelectedSSTableFormat(BIG)` at boot | `Main.bootstrapJvm()` |
| `!scanners.allScanners().stream().allMatch(s -> !s.isRangeScan())` | Always call `strategy.getScanners(actuallyCompact, null)` (no range) | `DirectCompactionRunner.run()` |
| `!metadata.all.stream().allMatch(SSTableReader::isLatestVersion)` | Only generate with `HarrySSTableWriter` (writes current version by default) | `DataGenerator` |
| `metadata.keyspaceName.equals(ACCORD_KEYSPACE_NAME)` | Never use `SchemaConstants.ACCORD_KEYSPACE_NAME` as keyspace | `SchemaGenerator` asserts |
| `!cfs.getPartitioner().supportsReusableKeys()` | Murmur3Partitioner.supportsReusableKeys() == true (verify in test) | `Main.bootstrapJvm()` |
| `metadata.indexes.size() != 0` | Never add secondary indexes; `CQLSSTableWriter.Builder` doesn't add indexes by default | `DataGenerator` + `SchemaGenerator` |
| `controller.tombstoneOption != NONE` | Never set `tombstone_compaction_interval`; UCS default option is NONE | `SchemaGenerator` verifies |

**Verification test:** `SchemaGeneratorTest.testAllGeneratedSchemasAreCursorCompatible()` generates 1000 schemas from random seeds and asserts `CursorCompactor.isSupported(scanners, controller)` returns `true` for all of them.

---

## 6. Risk Register

| # | Risk | Likelihood | Severity | Mitigation |
|---|---|---|---|---|
| R1 | **Split-package classloader issue** — `org.apache.cassandra.db.compaction` classes in `compaction-validator.jar` conflict with the same package in `cassandra.jar` on the classpath | Low (classpath mode, not module path) | High | Use the established precedent of `StressCQLSSTableWriter` in `org.apache.cassandra.io.sstable`. Verify at integration-test time. Both JARs are on flat classpath; no module-info.java. |
| R2 | **CompactionTask internal API changes** — the fields/methods used by `DirectCompactionRunner` (`task.cfs`, `task.gcBeforeSeconds`, `task.getCompactionController`, `task.finish`) are `protected`, not `public` | Medium (active codebase) | High | Add a comment block referencing each field + its line in `CompactionTask.java`. Add a unit test `DirectCompactionRunnerApiTest` that fails at compile time if the fields are renamed. |
| R3 | **Lanterna terminal compatibility** — Lanterna may not work in all terminal emulators (tmux, screen, non-ANSI) | Medium | Medium | Always provide `--no-ui` flag. Use `DefaultTerminalFactory` which auto-detects terminal capabilities. On detection failure, fall back to `PlainTextReporter`. |
| R4 | **Harry test-source dependency** — `HarrySSTableWriter` lives in `test/harry/main/` (test classpath only); using it in a production tool jar would require shipping test code in the release artifact | High | High | **Resolved by design**: use `CQLSSTableWriter` (main source) for all SSTable writing. The "Harry-based" choice means drawing on Harry's *conceptual model* (SchemaSpec, type variety) for `SchemaGenerator` and `RowGenerator`, not importing Harry classes at compile time. `TypeRegistry.java` independently catalogs types using `AbstractTypeGenerators.java` as reference. |
| R5 | **10 GiB /tmp exhaustion** — default working dir is `/tmp`; generating 10 GiB × 3 (source + 2 output) = 30 GiB minimum per run | High | High | Default to `--working-dir /tmp/cassandra-compaction-validator`. Print a startup warning if free space < 40 GiB. Allow `--target-bytes` to reduce to e.g. `500M` for quick sanity runs. |
| R6 | **UCS background compaction never converges** — `getNextBackgroundTasks` could theoretically return tasks indefinitely if new SSTables keep flushing | Low (we disable auto-flush/compaction) | High | After `cfs.addSSTables(...)`, call `cfs.disableAutoCompaction()`. Drive compaction exclusively through `getNextBackgroundTasks` in a single thread. Add a convergence timeout (default 30 min). |
| R7 | **CommitLog.instance.start() conflicts** with two CFS using same commitlog dir | Medium (offline CFS writes are typically skipped) | Medium | Offline compactions don't write to commitlog. But daemonInitialization() creates commitlog dirs. Use a dedicated tmpdir for `cassandra.commitlog.dir` set via system property before init. |
| R8 | **Harry SSTableWriter thread-safety** — `HarrySSTableWriter` uses `synchronized(HarrySSTableWriter.class)` in `build()` but each built writer instance is independent. Concurrent builds are fine; concurrent writes to the same instance are not | Low (each thread has its own writer) | Medium | Document: one `HarrySSTableWriter` instance per DataGenerator thread. Enforce in `DataGenerator` constructor. |
| R9 | **xxhash64 availability** — LZ4 library (`lz4-java`) is already in `lib/` and includes `net.jpountz.xxhash.XXHashFactory`. Use it instead of adding another dep | Low | Low | Verify `lib/lz4*.jar` exists: `ls lib/ | grep lz4`. If not present, use `java.util.zip.CRC32C` as fallback. |
| R10 | **UCS level visualization** — UCS doesn't use L0/L1/L2 level integers; it uses density/shard bucketing. The TUI `LsmTreeWidget` must derive "visual level" from `SSTableReader.getSSTableLevel()` or UCS's bucket metadata | Medium | Low | Use `sstable.getSSTableLevel()` as the visual level. UCS does set this field via `ShardedCompactionWriter`. If it returns 0 for all, fall back to grouping by size (log₂ of `sstable.onDiskLength()`). |

---

## 7. Test Plan

### 7.1 Unit tests (in `tools/compaction-validator/test/unit/`)

| Test class | What it tests |
|---|---|
| `SchemaGeneratorTest` | 1000 random schemas are all cursor-compatible; CQL is parseable; type variety |
| `DataGeneratorTest` | Target-bytes respected ±10%; all SSTable files exist and are non-empty |
| `ValidatorTest` | Validator passes when both sides are identical byte-streams; fails when one partition differs; `MismatchReport` has correct fields |
| `PartitionHasherTest` | Same partition stream produces same hash every time; different partition produces different hash |
| `DirectCompactionRunnerApiTest` | Compile-time-only: references `CompactionTask.cfs`, `CompactionTask.gcBeforeSeconds`, etc. to fail-fast if the production API changes |

### 7.2 Integration smoke test

```bash
# Build (ant wrapper)
.build/sh/ai-build

# Quick smoke run (500MB, one run, no TUI)
tools/bin/compaction-validator --target-bytes 500M --once --no-ui --working-dir /tmp/cv-smoke

# Expected: prints "PASS" and exits 0
```

### 7.3 Full 10 GiB run with TUI

```bash
tools/bin/compaction-validator --working-dir /tmp/cv-full
```
Expected: TUI renders without flickering; all phases complete; `PASS` displayed; auto-restarts after 7s.

### 7.4 Deterministic replay (failure simulation)

```bash
# Run with a known seed
tools/bin/compaction-validator --seed 0xDEADBEEF12345678 --once --no-cleanup

# Re-run with same seed, expect identical SSTables (byte-for-byte)
tools/bin/compaction-validator --seed 0xDEADBEEF12345678 --once --no-cleanup
```

### 7.5 CI invocation (for post-merge runs)

```bash
.build/sh/ai-ci-test org.apache.cassandra.tools.compactionvalidator.SchemaGeneratorTest
.build/sh/ai-ci-test org.apache.cassandra.tools.compactionvalidator.ValidatorTest
```

---

## 8. Implementation Phasing

Each milestone is independently buildable. Milestones 1-3 produce a working tool with no TUI; milestones 4-6 add the full TUI; milestone 7 is polish.

### Milestone 1: Build scaffold + bootstrap (no data, no compaction)
- `tools/compaction-validator/build.xml` (subproject, imports root)
- Edit `build.xml`: add import + wire to `_artifacts-init`, `build-test`
- `tools/bin/compaction-validator` launcher
- `Main.java` (picocli shell, `bootstrapJvm()`, prints "OK bootstrap" and exits)
- Add Lanterna to `cassandra-deps-maven-pom.xml` + `cassandra-build-maven-pom.xml`, run `ant resolver-retrieve-build`
- **Verify:** `ant compaction-validator-jar` succeeds; launcher runs and exits 0

### Milestone 2: PipelineAdapter — DirectCompactionRunner
- `PipelineSelector.java` (in `org.apache.cassandra.db.compaction`)
- `DirectCompactionRunner.java` (in same package)
- `DirectCompactionRunnerApiTest.java` — compile-time API verification test
- **Verify:** `ant compaction-validator-build-test` compiles; test passes

### Milestone 3: SchemaGenerator + DataGenerator (plain-text progress)
- `TypeRegistry.java`, `SchemaGenerator.java`, `GeneratedSchema.java`
- `DataGenerator.java`, `PartitionWriter.java`, `RowGenerator.java`, `DataGenStats.java`
- `RateTracker.java`, `ByteUtil.java`, `SeedUtil.java`
- `RunOrchestrator.java` — data-gen phase only, prints to stdout
- `SchemaGeneratorTest.java`, `DataGeneratorTest.java`
- **Verify:** Tool generates ~500M of SSTables in `/tmp/cv-smoke/source/`, prints counts

### Milestone 4: ParallelCompactor + Validator (plain-text output)
- `SstableSetManager.java`, `ParallelCompactor.java`, `CompactionDriver.java`, `CompactionStats.java`
- `Validator.java`, `PartitionHasher.java`, `PartitionComparator.java`, `MismatchReport.java`, `ValidationStats.java`
- Complete `RunOrchestrator` through all 4 phases
- `ValidatorTest.java`, `PartitionHasherTest.java`
- **Verify:** Full run completes; PASS/FAIL printed; timing stats shown

### Milestone 5: RunLogger + RunLoop + CLI (no TUI)
- `RunLogger.java`, `SystemInfo.java`
- `RunLoop.java`, `RunResult.java`, `RunSeed.java`
- Complete `Main.java` with all CLI flags
- **Verify:** Continuous loop runs; log file accumulates; `--once` exits; `--seed` replays

### Milestone 6: Lanterna TUI
- `ProgressBus.java`, `TuiEvent.java`
- `TuiManager.java` + all `*Panel.java` classes
- `LsmTreeWidget.java` (ASCII art LSM visualization)
- Wire `ProgressBus` into all existing phases
- `PlainTextReporter.java` (fallback for `--no-ui`)
- **Verify:** TUI renders in terminal; all panels update live; `--no-ui` works in piped context

### Milestone 7: Polish + risk mitigations
- `SchemaGeneratorTest.testAllGeneratedSchemasAreCursorCompatible()` (1000 schemas)
- Free-space startup warning
- Convergence timeout in `CompactionDriver`
- Performance multiplier display in `SummaryPanel`
- Test with UCS `scaling_parameters` variants to confirm visualization correctness
- **Verify:** 10-run soak test; no crashes; log file readable; TUI degrades gracefully on resize

---

## Critical File References

| Concern | Primary file | Key line(s) |
|---|---|---|
| Pipeline create (original) | `src/java/org/apache/cassandra/db/compaction/AbstractCompactionPipeline.java` | 34-52 |
| Cursor pipeline ctor | `src/java/org/apache/cassandra/db/compaction/CursorCompactionPipeline.java` | 38-43 |
| Iterator pipeline ctor | `src/java/org/apache/cassandra/db/compaction/IteratorCompactionPipeline.java` | 40-45 |
| runMayThrow lifecycle | `src/java/org/apache/cassandra/db/compaction/CompactionTask.java` | 170-380 |
| AbstractCompactionTask fields | `src/java/org/apache/cassandra/db/compaction/AbstractCompactionTask.java` | 33-37 |
| CompactionTask protected fields | `src/java/org/apache/cassandra/db/compaction/CompactionTask.java` | 83-89 |
| Offline CFS factory | `tools/stress/src/org/apache/cassandra/io/sstable/StressCQLSSTableWriter.java` | 624-690 |
| `HarrySSTableWriter` (reference only) | `test/harry/main/org/apache/cassandra/io/sstable/HarrySSTableWriter.java` | 1-658 (reference for patterns; NOT imported by tool) |
| SSTableReader.openNoValidation | `src/java/org/apache/cassandra/io/sstable/format/SSTableReader.java` | 381-393 |
| Partition merge iterator | `src/java/org/apache/cassandra/db/partitions/UnfilteredPartitionIterators.java` | 48 |
| CompactionStress reference | `tools/stress/src/org/apache/cassandra/stress/CompactionStress.java` | 1-376 |
| AbstractTypeGenerators (types) | `test/unit/org/apache/cassandra/utils/AbstractTypeGenerators.java` | 147-184 |
| UCS Controller options | `src/java/org/apache/cassandra/db/compaction/unified/Controller.java` | (option key constants) |
| Build template | `tools/sstableloader/build.xml` | 1-125 |
| Root build anchors | `build.xml` | 968, 1205, 1224, 1253, 1265, 1269, 1398, 1403, 2234, 2237, 2352 |
| isSupported gating | `src/java/org/apache/cassandra/db/compaction/CursorCompactor.java` | 118-156 |
| CursorCompactionEnabled flag | `src/java/org/apache/cassandra/config/DatabaseDescriptor.java` | 4804, 4810 |
