# compaction-validator

This is the build-from-scratch spec for the A/B compaction validator that lives in
`tools/compaction-validator/` and ships with the launcher script
`tools/bin/compaction-validator`. It captures the architecture, file layout,
data flow, configuration surface, JVM-global pitfalls, and notable design
decisions a reimplementer would need.

The original prose brief is in `spec.txt` at the repo root. The brief framed
this as "validate cursor compaction against legacy compaction"; the
implemented tool generalised that into an A/B framework — see §1 and §4.

---

## 1. Mission and Scope

Generate a deterministic Cassandra workload from a 64-bit seed, compact it
through two independently-configured pipelines (a **control** side and an
**experiment** side), and assert that the resulting SSTables produce
byte-/cell-identical partition streams. Run continuously, surface the run in a
Lanterna TUI plus an append-only log, and on validation failure preserve the
inputs + outputs and emit a deterministic re-run command.

The originally requested comparison was iterator-based vs cursor-based
compaction. The shipped tool can vary any of:

- **Compaction pipeline** — `ITERATOR` vs `CURSOR` (the original use case).
- **Compaction strategy** — `UnifiedCompactionStrategy`, `SizeTieredCompactionStrategy`, etc.
- **Compression codec** — `LZ4Compressor`, `ZstdCompressor`, `SnappyCompressor`, `DeflateCompressor`, with codec options.
- **`disk_access_mode`** — parsed and validated, but the two sides MUST agree
  on this value because `DatabaseDescriptor.disk_access_mode` is JVM-global.
  Per-side I/O modes need subprocess isolation, deferred.

Schema columns + data bytes are always identical across the two sides; the
varied attributes show up only in the per-side `CREATE TABLE` clauses.

---

## 2. Repository layout

```
tools/bin/
  compaction-validator                  launcher shell script
  cassandra.in.sh                       (modified) per-arch lib loader

tools/compaction-validator/
  build.xml                             ant subproject (imports root)
  configs/
    sample.yaml                         starting-point YAML
  src/
    org/apache/cassandra/db/compaction/
      PipelineSelector.java             enum + pipeline factory
      DirectCompactionRunner.java       same-package bridge into CompactionTask
    org/apache/cassandra/tools/compactionvalidator/
      Main.java                         picocli entry
      RunLoop.java                      seed-progressing outer loop
      RunOrchestrator.java              one-run orchestration
      RunResult.java                    immutable result record + Builder
      RunSeed.java                      SplitMix64-based seed progression
      SstableInfo.java                  POJO for events
      ProgressTap.java                  reporter interface
      PlainTextReporter.java            --no-ui sink
      compaction/
        CompactionDriver.java           drives one CFS through compaction
        CompactionStats.java            mutable counters
        ParallelCompactor.java          runs control + experiment in parallel
        SstableSetManager.java          per-run dir + hard-link layout
      config/
        RunConfig.java                  top-level YAML root
        RunSettings.java                run-loop knobs
        SchemaOverrides.java            shared schema overrides
        ComparisonConfig.java           control / experiment holder
        SideConfig.java                 per-side config
        CompactionSpec.java             {class, options} for compaction
        CompressionSpec.java            {class, options} for compression
        ConfigParser.java               SnakeYAML-backed loader
      data/
        DataGenerator.java              parallel CQLSSTableWriter driver
        DataGenStats.java               counters w/ monotonic estimate
        DataGeneratorDeterminismDriver.java   stand-alone determinism check
      logging/
        RunLogger.java                  append-only run log
        SystemInfo.java                 startup-header system stats
      schema/
        SchemaGenerator.java            seed-driven schema generator
        GeneratedSchema.java            schema + per-side CQL builder
        ColumnInfo.java                 column descriptor POJO
      tui/
        TuiManager.java                 Lanterna lifecycle + render loop
        TuiPanel.java                   panel SPI
        TuiEvent.java                   sealed event hierarchy
        ProgressBus.java                event queue
        HeaderPanel.java                title/seed/phase/heap row
        SchemaPanel.java                CQL display
        DataGenPanel.java               progress bar + counters + rates
        CompactionPanel.java            side-by-side LSM widgets
        LsmTreeWidget.java              size-tier grouped sstable display
        ValidationPanel.java            per-side validation bars
        SummaryPanel.java               post-run summary + failure inspector
        RunHistoryPanel.java            recent-runs ledger at the bottom
        RateMeter.java                  EWMA throughput tracker
        TuiUtil.java                    formatting helpers
      util/
        ByteUtil.java                   parse "10G", format "1.2 GiB"
        SeedUtil.java                   per-component seed derivation
        RateTracker.java                EWMA helper
      validation/
        Validator.java                  Phase A / Phase B driver
        PartitionHasher.java            xxhash64 over Unfiltered stream
        PartitionComparator.java        cell-by-cell deep equals
        MismatchReport.java             mismatch detail bundle
        ValidationStats.java            atomic counters + errata map
        ErrataRule.java                 known-bug catalogue (enum)
        ErrataChecker.java              rule-based partition matcher
  test/unit/
    org/apache/cassandra/db/compaction/
      DirectCompactionRunnerApiTest.java
    org/apache/cassandra/tools/compactionvalidator/
      compaction/SstableSetManagerTest.java
      data/DataGeneratorDeterminismTest.java
      validation/ErrataCheckerTest.java
      validation/PartitionHasherTest.java
      validation/ValidatorTest.java
```

Modified files outside the subproject:

- `build.xml` — adds `cv.build.classes`/`cv.test.classes` to the test classpath
  in three places, wires `compaction-validator-jar` into `jar`,
  `compaction-validator-build-test` into `build-test`, adds an `<import>` for
  `tools/compaction-validator/build.xml` at the bottom.
- `.build/parent-maven-pom.xml` — `<dependencyManagement>` entry for
  `com.googlecode.lanterna:lanterna:3.1.2`.
- `.build/cassandra-deps-maven-pom.xml` — corresponding `<dependency>` entry
  (no version — managed via parent).
- `tools/bin/cassandra.in.sh` — adds `lib/<arch>/` to classpath with
  `-linux-`/`-osx-` filename filtering (see §13).

---

## 3. CLI surface

`tools/bin/compaction-validator` sources `cassandra.in.sh`, sets up
`cassandra.storagedir` under `<working-dir>/_cassandra-system` (wiped on every
launch), and execs the JVM with `org.apache.cassandra.tools.compactionvalidator.Main`.

Picocli flags:

| Flag | Required | Purpose |
|---|---|---|
| `--config / -c <path>` | yes | YAML config (§4) |
| `--seed / -s <hex|dec>` | no | override the per-run seed |
| `--working-dir / -w <path>` | no | working-dir base (default `${TMPDIR}/cassandra-compaction-validator`) |
| `--log-file / -l <path>` | no | append-only log file (default `./compaction-validator.log`) |
| `--once` | no | run a single iteration then exit (overrides YAML `max_runs`) |

Everything else (target bytes, thread counts, ignore-errata, no-cleanup,
no-ui, max-runs) lives in YAML.

Launcher knobs via env:

- `MAX_HEAP_SIZE` — defaults to `24G`.
- `ANTHROPIC_GC_ALGO` — defaults to `-XX:+UseZGC -XX:+ZGenerational -XX:+ZUncommit`
  (requires JDK 21+); set to `-XX:+UseG1GC` on JDK 17.
- `CV_GC_LOG` — JVM unified-GC-log path (default `/tmp/compaction-validator-gc.log`).
- `JAVA`, `CLASSPATH`, `JVM_OPTS`, `JAVA_AGENT` — sourced from `cassandra.in.sh`.

---

## 4. YAML config (`configs/sample.yaml`)

```yaml
version: 1                                # forward-compat schema version

run:                                       # run-loop / environment
  target_bytes: 10G                        # ByteUtil.parseBytes; "10G", "500MiB", etc.
  datagen_threads: 8                       # parallel CQLSSTableWriter writers
  compaction_threads: 4                    # concurrent compaction tasks PER SIDE
  validation_threads: 12                   # token-range Phase A shards
  max_runs: 0                              # 0 = unlimited
  no_cleanup: false                        # preserve dirs even on success
  no_ui: false                             # use plain stdout instead of TUI
  ignore_errata:                           # known-bug rules to suppress
    - equal-ts-tiebreaker

schema:                                    # cross-side schema overrides
  partitioner: Murmur3                     # only Murmur3 supported by cursor

comparison:
  control:                                 # baseline / "known good" side
    name: legacy                           # display label (logs, TUI, mismatch reports)
    pipeline: ITERATOR                     # ITERATOR | CURSOR (with aliases)
    compaction:                            # WITH compaction = { ... }
      class: UnifiedCompactionStrategy
      options:
        target_sstable_size: 64MiB
        base_shard_count: 4
    compression:                           # WITH compression = { ... }
      class: LZ4Compressor
      chunk_length_in_kb: 16               # flat options OR `options: { ... }`
    io_mode: standard                      # disk_access_mode value

  experiment:                              # variation under test
    name: cursor
    pipeline: CURSOR
    compaction: { class: UnifiedCompactionStrategy, options: { ... } }
    compression: { class: LZ4Compressor, chunk_length_in_kb: 16 }
    io_mode: standard                      # MUST equal control.io_mode
```

### Behaviour

- **Required structure**: `version`, `run`, `comparison.control`,
  `comparison.experiment`. Missing → `IllegalArgumentException` from
  `RunConfig.validate()` with a one-line user-facing message.
- **Unknown YAML keys**: rejected fast in `ConfigParser` with `Unknown key 'X'
  under <section>. Allowed: [...]`. Typos surface immediately.
- **Omitted attributes on a side**: inherit the **seed-picked** value applied
  identically to BOTH sides. `SchemaGenerator` produces `defaultCompaction`
  and `defaultCompression` per seed; `GeneratedSchema.buildCqlForSide()`
  overlays the side's overrides on those defaults.
- **`io_mode` parity**: required for now (validated in `RunConfig.validate`).
  Per-side I/O modes require subprocess isolation since
  `DatabaseDescriptor.disk_access_mode` is JVM-global.
- **CLI overrides**: `--seed`, `--working-dir`, `--log-file`, `--once`
  override the YAML / per-run defaults. No way to override individual YAML
  fields from the CLI — by design, the YAML is the source of truth.

### YAML parser

`ConfigParser` is hand-rolled (uses SnakeYAML only as a Map/List loader, not
for bean binding). Each section has its own `bind*(Map<String,Object>)`
function that:
- Coerces values via `asString`/`asInt`/`asBool`/`asMap` helpers (with
  friendly error messages on type mismatch).
- Calls `rejectUnknownKeys(map, sectionName, allowed...)` to fail fast on
  typos.
- Builds the typed config object.

Snake-case YAML keys map directly to camelCase Java fields by hand (e.g.
`target_bytes` → `targetBytes`). No reflective magic.

---

## 5. Pipeline phases

Per run, the orchestrator (`RunOrchestrator.execute`) drives:

### Phase 0 — directory prep
`SstableSetManager` creates `<working-dir>/<seed-hex>/` with
`source/`, `output-legacy/`, `output-cursor/`. (The `legacy`/`cursor`
directory names are routing slots, not display labels; see §10.)

### Phase 1 — schema generation
`SchemaGenerator.generate(rootSeed)`:
- 1–2 partition-key columns from `{int, bigint, uuid, ascii, text}`
- 0–2 clustering-key columns with random ASC/DESC; if any STATIC columns are
  picked but ckCount==0, statics drop (CQL doesn't allow them)
- 0–2 STATIC columns from the regular type set
- 3–8 regular columns from
  `{int, bigint, uuid, ascii, text, timestamp, boolean, float, double, blob,
    varint, decimal, frozen<set<text>>, frozen<list<int>>,
    frozen<map<text,int>>, tuple<int,text>}`
  (collections frozen because `CursorCompactor.unsupportedSchema` rejects
  non-frozen / "complex" columns)
- UCS defaults: `target_sstable_size` ∈ {64, 128, 256, 512} MiB,
  `base_shard_count` ∈ {1, 2, 4}
- LZ4 chunk size ∈ {4, 16, 64, 256, 1024} KiB
- Keyspace name `cvtest_<low32(seed)>`, table name `t`, deterministic
  `TableId` derived from `keyspace.tableName` for predictable disk paths
- Returns `GeneratedSchema` with column lists + `defaultCompaction` /
  `defaultCompression` specs (populated even when not testing those axes).

`GeneratedSchema.buildCqlForSide(keyspaceOverride, side)` overlays the side's
specs on the defaults and emits `CREATE TABLE`.

### Phase 2 — data generation
`DataGenerator` runs `datagen_threads` parallel `CQLSSTableWriter` instances,
each with its own thread seed (`SeedUtil.threadDataSeed(dataSeed, i)`) and
its own per-thread byte quota (`ceil(target / threads)`). Per-thread quotas
guarantee determinism — an earlier shared `done` flag tied to a global byte
counter raced on `AtomicLong` updates from per-thread flush listeners and
made the same `(seed, threads)` produce slightly different per-thread
partition counts.

Per-thread RNG draws (deterministic given the seed):
1. `maxSstableMib` (range `[1, 512]`)
2. Loop:
   - `rowsPerPartition = 1 + nextInt(16)`
   - For each row: `Object[] values = buildRowValuesStatic(schema, rng)`
     (PK columns never null; non-PK columns null with `NULL_FRACTION = 0.10`)

`writer.addRow(values)` → CQLSSTableWriter buffers and flushes when the
buffer hits `maxSstableMib`. The on-flush listener releases SSTableReader
refs immediately to avoid file-descriptor pressure.

`DataGenStats`:
- `bytesWritten`, `partitionsWritten`, `rowsWritten` — atomic
- `rowsAtLastFlush` — captured per flush so `estimatedBytesWritten()`
  extrapolates in-flight bytes from rows written since the last flush
- `estimatedHighWatermark` — running max so `estimatedBytesWritten()` is
  monotonically non-decreasing (the rough 64-byte-per-row default would
  otherwise snap downward when the first flush calibrates a smaller actual
  bytes/row).

### Phase 3 — per-side CFS provisioning
For each side:
1. Sanitize side name → CQL identifier (lowercase, non-`[a-z0-9_]` →
   `_`, must start with a letter)
2. Per-side keyspace = `<baseKs>_<sanitized>`
3. Compute deterministic `TableId` for `<perSideKs>.t`
4. Hard-link source SSTables to the expected on-disk path
   `<sideDir>/<keyspace>/<table>-<tableId>/` BEFORE constructing the CFS.
   The SSTable id generator scans the dir during CFS construction and
   advances past existing files; if we hard-linked AFTER construction, the
   generator would start at 1 and collide with the pre-existing
   `pa-1-big-*.db` files.
5. `Schema.instance.submit(addKeyspace + addTable)` for the per-side keyspace
6. `Keyspace.openWithoutSSTables` + `ColumnFamilyStore.createColumnFamilyStore`
   pointing at the per-side directory
7. `SSTableReader.openNoValidation` for each `Data.db` file; `cfs.addSSTables`
8. `cfs.disableAutoCompaction()` — the orchestrator drives compaction
   explicitly

### Phase 4 — parallel compaction
`ParallelCompactor` runs two `CompactionDriver` instances in a 2-thread pool,
one per side. Each fires `reporter.onCompactionComplete(backend, stats)` the
instant its driver returns so a side that finishes first immediately freezes
its rate metrics (avoids the rate counter visibly drifting toward zero while
the other side is still going).

`CompactionDriver`:
- Accepts a `taskConcurrency` per backend (config `compaction_threads`).
- Outer loop: `getNextBackgroundTasks(now)` until empty, with
  `MAX_ITERATIONS = 1000` cap as anti-spin
- Per round: submit each `CompactionTask` to a fixed-size pool, wait for all
  futures (workers run in parallel up to `taskConcurrency`)
- Non-`CompactionTask` subtypes (e.g. `SingleSSTableLCSTask`) bypass the
  pipeline-selectable runner and execute on the caller's thread
- Final maximal pass via `getMaximalTasks(...)` to converge
- `LifecycleTransaction.waitForDeletions()` between rounds and after the
  maximal pass to drain obsoleted SSTables — without this the next round
  sees the obsolete inputs alongside the new outputs and re-compacts both,
  blowing the output dir up to `N × input` after `N` rounds
- `stats.sstablesOut = cfs.getLiveSSTables().size()` (NOT cumulative
  `emitted.size()` — multi-pass UCS produces transient sstables that get
  obsoleted by later passes; `emitted` accumulates them all)
- Per-task progress aggregated via `InFlightTracker` (a
  `ConcurrentHashMap<taskIndex, Counters>`); reporter events sum across all
  in-flight tasks so the per-side "current task" bar reflects "work
  currently underway"

`DirectCompactionRunner` (in `org.apache.cassandra.db.compaction` package
specifically so it can access `CompactionTask`'s `protected` fields):
- `inputSSTablesOf(ct)` and `targetLevelOf(ct)` static helpers expose
  task internals
- Runs the per-task lifecycle: `task.getCompactionController` →
  `getMaximalTasks/getScanners` → builds an `AbstractCompactionPipeline` via
  `PipelineSelector.create(backend, ...)` → opens the
  `CompactionAwareWriter` from `task.getCompactionAwareWriter` → loops
  `pipeline.processNextPartitionKey()` → `task.finish(pipeline)`
- Periodically (every ~1MB) invokes `ProgressCallback.onProgress(...)` and
  `onSStableEmitted(...)` so the TUI gets live throughput

`PipelineSelector.Backend` enum: `ITERATOR`, `CURSOR`. `create(...)` selects
between `IteratorCompactionPipeline` and `CursorCompactionPipeline`,
bypassing the global `cursorCompactionEnabled` flag.

### Phase 5 — validation
`Validator.validate()`:
- **Phase A (parallel hash sweep)**: split the Murmur3 ring into
  `validation_threads` non-overlapping `Bounds<PartitionPosition>` segments
  (BigInteger arithmetic to avoid overflow at the 2^64-1 ring width). Each
  shard opens scanners for ALL output SSTables on each side restricted to
  its bounds, merges via `UnfilteredPartitionIterators.merge`, and walks
  both sides in lockstep. Per partition: hash via `PartitionHasher.hash...`
  (xxhash64 — really an FNV-style fold over `Unfiltered.toString` /
  serialized bytes, deterministic) and compare. On mismatch the shard
  returns the partition key. Aggregator picks the **smallest-token**
  mismatch across all shards as the canonical report — keeps reproductions
  deterministic regardless of shard scheduling.
- **Phase B (deep compare)**: re-opens both sides' scanners restricted to
  the offending partition's bounds and runs `PartitionComparator.compare`,
  producing a `MismatchReport` that walks partition key → partition-level
  deletion → static row → body unfiltereds → row clustering / liveness /
  deletion / cells (column metadata, value bytes, timestamp, TTL,
  localDeletionTime, counter flag, complex path). First difference wins.

`ValidationStats`:
- `partitionsChecked`, `rowsChecked`, `partitionMismatches` — `AtomicLong`
  (concurrent shard updates)
- `errataOccurrences: EnumMap<ErrataRule, AtomicLong>` — pre-populated for
  all rules, incremented from the Validator's hash-mismatch path when
  `ErrataChecker.matches(...)` fires (§8)
- `durationMs` — single writer (Validator's outer block), volatile long
- `hasMismatches()` returns `true` only when `partitionMismatches > 0` —
  errata-suppressed partitions don't count.

### Phase 6 — result assembly + cleanup
- `RunResult.Builder` is populated through every phase; the orchestrator
  builds the final immutable `RunResult` and fires `reporter.onRunComplete`
  or `onRunFailed` (with the `MismatchReport`).
- On success and `cleanupOnSuccess`, the per-run dir is wiped AFTER
  `cfs.invalidate(false, dropData=true)` — invalidate writes a transaction
  log that needs the dir to still exist.
- On failure, the per-run dir is preserved and the path is in
  `result.preservedDir`.
- Every run, `tryDropKeyspace` removes both per-side keyspaces from
  `Schema.instance` and `cfs.invalidate` releases SSTableReader refs;
  without this, `Ref.OnLeak` logs `LEAK DETECTED` for every output reader
  on the next GC and `Schema.instance` accumulates a keyspace per run for
  the lifetime of the process.

---

## 6. Determinism contract

A 64-bit root seed flows through every randomised decision via `SeedUtil`:

```
rootSeed
├── schemaSeed = root ^ 0x1111111111111111
│     drives column counts/types, clustering ASC/DESC, compaction defaults
├── dataSeed   = root ^ 0x2222222222222222
│     split per thread via threadDataSeed
└── validatorSeed = root ^ 0x3333333333333333  (reserved)
```

`threadDataSeed(dataSeed, threadIndex)` uses the **SplitMix64 finalizer**.
Earlier code XOR'd thread variation into the high 16 bits — Java's
`Random(long)` constructor masks with `((1L << 48) - 1)`, so high-bit
variation got discarded and EVERY THREAD GENERATED IDENTICAL DATA. This is
the bug `DataGeneratorDeterminismDriver` caught and the fix is in
`SeedUtil.threadDataSeed`. SplitMix64 spreads variation across all 64 bits.

Same `(seed, threads, target)` produces:
- Same `GeneratedSchema` (columns, types, defaults)
- Same per-thread partition + row counts
- Same RNG-driven row values (timestamps in CQLSSTableWriter use wall-clock
  microseconds — NOT deterministic — so byte-level SSTable identity is not
  guaranteed across runs; logical content equality IS)

`RunSeed.nextSeed(seed, runNumber)` uses the SplitMix64 finalizer too — each
new run gets a wildly different seed from the previous one.

---

## 7. Cursor-compatibility constraints

`CursorCompactor.isSupported(scanners, controller)` rejects:
- Non-`BigFormat` SSTables → `Main.bootstrapJvm` calls
  `setSelectedSSTableFormat(BigFormat.getInstance())`
- Range scanners → `getMaximalTasks` is given the full set, no
  `getScanners(set, range)` calls in this tool's path
- Non-current-version SSTables → the validator only writes fresh sstables
  via `CQLSSTableWriter` so they're current by construction
- `keyspaceName == ACCORD_KEYSPACE_NAME` → `cvtest_<seed>` never matches
- Partitioner without `supportsReusableKeys()` → `Main.bootstrapJvm` calls
  `setPartitionerUnsafe(Murmur3Partitioner.instance)`
- Non-empty `metadata.indexes` → `SchemaGenerator` never adds indexes
- `controller.tombstoneOption != NONE` → schemas don't set
  `tombstone_compaction_interval`; UCS default option is NONE

`CursorCompactor.unsupportedSchema` rejects non-frozen ("complex") columns,
counter columns, vector columns. The schema generator's type catalogue is
filtered accordingly.

---

## 8. Errata rules (`--ignore-errata`)

Mechanism for tolerating known cursor-compaction bugs while a fix is
in-flight. When a Phase A hash mismatch's per-cell differences are fully
explained by an active rule, the validator records an occurrence under that
rule and continues — the run still PASSes if no other divergence is found.
Any new divergence still halts loudly.

### Rules (as of writing)

`ErrataRule.EQUAL_TIMESTAMP_TIEBREAKER` — CLI name `equal-ts-tiebreaker`.

Discovered 2026-05-25 against seed `0x693F6A0B09368F0C` partition `wUZ`.
`CursorCompactor.mergeCells()`'s `COMPARE` branch inverts the tie-breaker:
when two cells share a timestamp, TTL, and `localDeletionTime` but differ in
value bytes, cursor picks the cell with the **smaller** bytes; legacy
correctly picks the larger one per `Cells.resolveRegular()`'s
`compareValues(left,right) >= 0 ? left : right`.

The rule's matcher predicate fires when:
- `legacy.timestamp() == cursor.timestamp()`
- `legacy.ttl() == cursor.ttl()`
- `legacy.localDeletionTime() == cursor.localDeletionTime()`
- `legacy.isCounterCell() == cursor.isCounterCell()`

(Anything else is treated as a different defect and not suppressed.)

### Architecture

- `ErrataRule` enum — each entry has `cliName()`, `description()`, and a
  per-rule `explainsCellValueDifference(legacy, cursor)` predicate. CLI
  parsing via `ErrataRule.parseCliList(commaSeparated)` rejects unknown
  names with the full known-rule list in the error message.
- `ErrataChecker` — given a partition pair, walks both sides in lockstep
  (mirroring `PartitionComparator`'s structure) and asks each active rule
  whether each cell-pair difference fits its predicate. If EVERY
  difference is covered by SOME rule, returns the set of fired rules. Any
  unrelated difference (length mismatch, partition-level deletion diff,
  row liveness diff, range-tombstone diff, complex column data, etc.)
  returns the empty set → caller treats as a real mismatch.
- `Validator.checkErrataForPartition(key)` — re-scans just that partition
  with bounds-restricted scanners (cheap), runs the matcher, and returns
  the rule set.
- `ValidationStats.errataOccurrences` — `EnumMap<ErrataRule, AtomicLong>`
  pre-populated for all rules. Multiple shards bump counters via
  `incrementAndGet` without locking.
- Reports show errata loudly:
  - `PlainTextReporter.onValidationComplete` prints a Unicode-bordered
    block listing each rule + count when total > 0.
  - `RunLogger` writes an `Errata: N suppressed (run still PASS):` block
    with per-rule counts.

`ErrataChecker.matches` uses a name+kind+position column-equality check
rather than `ColumnMetadata.equals` because the two sides come from
different keyspaces (different `ksName`) and the default equals check
would never agree.

---

## 9. ProgressTap / TUI event model

`ProgressTap` is the reporter SPI. Two implementations: `TuiManager` (drains
events into a Lanterna screen at ~1 Hz) and `PlainTextReporter` (writes to a
`PrintStream` with rate-limited progress lines).

### Events (chronological in a run)

- `onRunStart(runNumber, seed)` — reset all panels
- `onComparisonLabels(controlName, experimentName)` — distribute display
  labels for the run; routes through `TuiEvent.ComparisonLabels`. Internal
  routing tags on later events stay `"legacy"`/`"cursor"` (control / experiment
  slot identifiers); panels translate tag → label at render time.
- `onSchemaReady(GeneratedSchema)` — populate Schema panel
- `onDataGenProgress(bytes, target, partitions, rows, bps)` — every 250 ms
  via a daemon ticker in `RunOrchestrator`; reads `DataGenStats` snapshots
- `onDataGenComplete(stats, durationMs)` — final progress + freeze rates
- `onCompactionInputs(backend, inputs)` — pre-populates LSM widget
- `onCompactionTaskStart(backend, taskInputs, targetLevel, taskIndex,
  tasksInBatch, totalTasksDone)`
- `onCompactionProgress(backend, bytesCum, target, partitionsCum, bps,
  currentTaskBytes, currentTaskTotal)` — per-task bytes/total are SUMS
  across in-flight tasks per side
- `onSstableEmitted(backend, filename, sizeBytes, level)`
- `onCompactionTaskEnd(backend, taskIndex, taskOutputs)` — `taskIndex`
  matches the corresponding `Start` event; lets panels disambiguate
  out-of-order completions under `compaction_threads > 1`
- `onCompactionComplete(backend, stats)` — fired the instant each side's
  driver returns (NOT after both finish)
- `onValidationProgress(partitionsChecked)` — every 250 ms via the
  validation ticker
- `onValidationComplete(stats, success)` — final stats + PASS/FAIL
- `onRunComplete(result)` or `onRunFailed(report, result)`

### TUI layout (Lanterna)

`TuiManager` runs at `TARGET_FPS = 1` (1 Hz). The render loop:
1. Drains the `ProgressBus` queue and dispatches events to all panels.
2. Polls keystrokes (`q` / Esc / Ctrl-C → `quitRequested`; ↑/↓/PgUp/PgDn/
   Home/End → SummaryPanel scroll when in failure mode).
3. **Wipes the back-buffer to spaces with a full-screen `fillRectangle`**
   before rendering. Without this, when the schema panel's height changes
   between runs (different schemas have different CQL line counts), every
   panel below it shifts and characters drawn at the OLD position survive
   in the back-buffer. `TerminalScreen.refresh()` only sends changed cells,
   so re-rendering with the same final state produces no diff and no
   flicker — the wipe is a clean way to handle layout shifts.
4. Renders panels into a vertical layout with **1-row gaps** between
   adjacent visible panels (header → schema → data-gen → compaction →
   validation → history; gap omitted before/after a 0-height panel).
5. Renders a footer row with quit/scroll hints and a heap-usage indicator
   (right-aligned, yellow ≥70%, red ≥90%).

When the `SummaryPanel` is active (post-run summary, or sticky on failure),
the regular layout is replaced by header + summary + footer.

Layout calculation (`computeLayout`):
- Reserve 1 row footer + 5 worst-case gap rows from `available`.
- Preferred heights: schema = `min(schemaCqlLines, max(8, rows/2))`,
  dataGen = 4, compaction = 10, validation = 3, history = 6.
- When `available < total`, drop in priority order: history, schema,
  data-gen, validation, compaction (compaction shrinks last and never
  below 1).

### Panel highlights

- **HeaderPanel**: title, run #, seed, phase, elapsed.
- **SchemaPanel**: scrollable CQL display; reports `getDesiredHeight()`
  back to the layout so the schema panel sizes itself to the actual CQL.
- **DataGenPanel**: progress bar (`bytes/target`), counters, rates (EWMA via
  `RateMeter`).
- **CompactionPanel**: side-by-side LSM widgets with size-tier-grouped
  sstables (log2(MiB) bucketing, since UCS doesn't use level integers
  per-sstable). Per-side throughput row uses `scanned` (NOT "total") to
  honest about cumulative-across-passes semantics.
- **LsmTreeWidget**: per-tier rows with sstable boxes scaled by size; pulses
  the active marker `✦` while `markActive()`/`markInactive()` driven by
  progress events.
- **ValidationPanel**: two progress bars (control, experiment) sharing one
  partitions counter (both sides verify the same set, so identical progress
  is correct). Denominator uses the data-gen unique-partition count, NOT
  the compaction `partitionsProcessed` (which is cumulative across passes
  and would make 4% mean "we've checked 200% of unique partitions").
- **SummaryPanel**: 7-second post-run banner (success), or sticky red
  failure-inspection view with full mismatch detail (description, schema
  CQL, per-side partition dumps from `MismatchReport`); supports
  ↑/↓/PgUp/PgDn/Home/End scroll.
- **RunHistoryPanel**: bottom-of-screen ledger of recent runs (start time,
  finish time, duration, seed, data size, PASS/FAIL).

Side display labels — captured via `ComparisonLabels` event, default
`"LEGACY"` / `"CURSOR"`. Internal routing tags on event `backend` fields
remain `"legacy"`/`"cursor"` (treated as opaque slot identifiers).

---

## 10. Internal routing tags vs display labels

Throughout the codebase:
- The strings `"legacy"` and `"cursor"` are **routing tags** that identify
  the control slot and experiment slot respectively. They appear in event
  `backend` fields, in `RunResult.legacyStats`/`.cursorStats` field names,
  and inside `ParallelCompactor`/`CompactionDriver`.
- The names users see in TUI panels, plain-text logs, and the run log are
  **display labels** — taken from `comparison.control.name` and
  `comparison.experiment.name`. They flow through `ProgressTap.onComparisonLabels`.

This separation lets the validator support arbitrary side names ("stcs",
"ucs-cursor", "lz4-vs-zstd") without renaming routing IDs everywhere. A
`labelFor(backend)` helper in each consumer translates the tag to the
display label.

---

## 11. Plain-text reporter (`--no-ui` / no TTY)

`PlainTextReporter`:
- Throttles per-side compaction-progress lines to 1/sec.
- Output format examples:
  ```
  [run 1] Starting run with seed=0x...
  [run] comparison: control=legacy  experiment=cursor
  [schema] keyspace=cvtest_xxx table=t pk=2 ck=1 static=2 regular=5
  [datagen] 28.1 MiB/10.0 GiB (0.3%)  partitions=54651 rows=460615  109.8 MiB/s
  [compact:legacy]   task 1 of 2 started: 4 input sstables, 338.2 MiB
  [compact:cursor]   task 1 of 2 started: 4 input sstables, 338.2 MiB
  [compact:cursor]   349.0 MiB cumulative  partitions=1512000  333.4 MiB/s  task 1 of 2 (53%)
  [compact:cursor]   complete: 3.2s (228.8 MiB/s, 1417300 partitions)
  [validate] complete: PASS (708807 partitions, 1410595 rows)
  [run 1] PASSED in 20.6s (cursor speedup 2.34x)
  ```
- On failure, dumps the full `MismatchReport` via a section header:
  schema CQL, then `--- CONTROL OUTPUT ---` + control dump, then
  `--- EXPERIMENT OUTPUT ---` + experiment dump.
- Errata block (when `totalErrataOccurrences > 0`) is a Unicode-bordered
  box with per-rule counts.

---

## 12. Run log (`compaction-validator.log`)

`RunLogger` opens with `APPEND | CREATE`, never closes until process exit.
On start, writes a header:

```
================================================================================
[2026-05-24T18:48:45Z] compaction-validator started
  Host:    hostname
  OS:      Linux 6.18.5
  JVM:     OpenJDK 21
  CPU:     32 cores
  Memory:  128GiB
================================================================================
```

Per run entry:

```
[2026-05-24T18:49:09Z] Run #47 seed=0xDEADBEEF12345678
  Schema:  cvtest_deadbeef.t  (2 PK, 1 CK, 2 static, 5 regular cols)
  UCS:     target_sstable_size=256MiB  base_shard_count=2  compression_chunk=16KiB
  Schema CQL:
    CREATE TABLE cvtest_deadbeef.t (
      ...
    )
  DataGen: 10.3 GiB  |  1.4M partitions  |  52M rows  |  310 MiB/s avg
  legacy:    18.4s  |  290 MiB/s  |  1.4M partitions/s
  cursor:    15.7s  |  340 MiB/s  |  1.6M partitions/s  |  Speedup: 1.17x
  Valid:   PASS  |  1.4M partitions matched  |  52M rows matched
  Errata:  3 suppressed (run still PASS):
    equal-ts-tiebreaker             3
```

Side labels are user-supplied (control name and experiment name), padded to
8 chars + `:` for column alignment via `padLabel(name)` (truncates over-long
names to 7 chars + `.`).

On failure: Valid line says `FAIL  |  <description>`, plus `Preserved:` +
`Re-run:` lines and a full `Mismatch detail:` block (the
`MismatchReport.formatForDisplay` output, indented 4 spaces).

---

## 13. JVM bootstrap + global state

`Main.bootstrapJvm()` runs ONCE per process, before picocli parses args:

1. `DatabaseDescriptor.daemonInitialization()` — needed for ColumnFamilyStore
   construction; lighter `clientInitialization` is insufficient.
2. `CommitLog.instance.start()` — required by step 3, even though we never
   write to the commitlog (offline tool). `cassandra.storagedir` is set to
   `<workingDir>/_cassandra-system` and wiped at launch by the shell script
   so commitlog segments don't leak across launches.
3. `ClusterMetadataService.initializeForTools(false)` — `false` means don't
   load any pre-existing system_schema SSTables. With `true`, a stale
   transaction log left over from a prior crash makes startup fail with
   "inconsistent disk state". With `false` and a fresh storagedir, CMS
   bootstraps in seconds.
4. `Keyspace.setInitialized()`
5. `setSelectedSSTableFormat(BigFormat.getInstance())` — cursor only
   supports BIG.
6. `setPartitionerUnsafe(Murmur3Partitioner.instance)` — required by
   `CursorCompactor.isSupported`.
7. `setCompactionThroughputBytesPerSec(0)` — disable the 64 MiB/s default
   throttle that would silently cap measured throughput.

JVM-global state caveats:
- `disk_access_mode` is process-global; sides must agree (§4).
- `Schema.instance` accumulates keyspaces across runs; orchestrator's
  `tryDropKeyspace` cleans them up after each run.
- `Ref.OnLeak` watchdog logs `LEAK DETECTED` for every output reader
  whose CFS goes out of scope without `invalidate(false, dropData=true)`.
  Trade-off: with `--no-cleanup`, `dropData=false` keeps the output sstables
  on disk for inspection but produces leak warnings on the next GC.

---

## 14. ACCP / per-arch native libraries

Both `tools/bin/cassandra.in.sh` and `tools/bin/compaction-validator` add
`$CASSANDRA_HOME/lib/<arch>/` to the classpath. Filename filtering by
upstream classifier infix:

- File contains `-linux-` → included only when `uname -s = Linux`
- File contains `-osx-` → included only when `uname -s = Darwin`
- Anything else (no OS infix) → included unconditionally

Architecture aliases: `arm64` → `aarch64`, `amd64` → `x86_64`. So macOS
arm64 maps `uname -m = arm64` → look in `lib/aarch64/` for `*-osx-aarch_64.jar`.

Both jars (`AmazonCorrettoCryptoProvider-2.2.0-linux-aarch_64.jar` and
`AmazonCorrettoCryptoProvider-2.2.0-osx-aarch_64.jar`) can coexist in
`lib/aarch64/`; the script picks the right one for the current host. ACCP
fails its native health check noisily if a Linux-binary JAR ends up on a
macOS JVM's classpath, so this filter matters.

---

## 15. Build integration

### Subproject (`tools/compaction-validator/build.xml`)

- Imports root `build.xml`.
- Targets:
  - `compaction-validator-build` — `javac` over `tools/compaction-validator/src/`
    into `${build.classes}/compaction-validator/`. Classpath:
    `cassandra.classpath` (main jar + lib).
  - `compaction-validator-build-test` — `javac` over
    `tools/compaction-validator/test/unit/` into
    `${build.dir}/test/compaction-validator-classes/`. Classpath:
    `cassandra.classpath.test` + `${cv.build.classes}` + lanterna jar.
  - `compaction-validator-test` — `testmacro` over `${cv.test.src}` with
    default heap.
  - `compaction-validator-jar` — bundles `${cv.build.classes}` into
    `${build.dir}/tools/lib/compaction-validator.jar` with
    `Main-Class: org.apache.cassandra.tools.compactionvalidator.Main`.

### Root build (`build.xml`)

- `<import file="${basedir}/tools/compaction-validator/build.xml"/>` near
  the bottom (after sstableloader's import).
- `_artifacts-init` / `jar` target depends on `compaction-validator-jar`.
- `build-test` target depends on `compaction-validator-build-test`.
- `${cv.build.classes}` and `${cv.test.classes}` added to the test
  classpath in three places (`_build-test`, `check-test-names`, the
  `cassandra.classpath.test.runtime` path).

### POMs

`com.googlecode.lanterna:lanterna:3.1.2` added to:
- `.build/parent-maven-pom.xml` `<dependencyManagement>` (with version)
- `.build/cassandra-deps-maven-pom.xml` `<dependency>` (no version, managed)

`ant resolver-retrieve-build` downloads `lib/lanterna-3.1.2.jar` afterwards.

---

## 16. Tests

| Test | Coverage |
|---|---|
| `DirectCompactionRunnerApiTest` | Compile-time guard that `CompactionTask`'s protected fields haven't been renamed |
| `SstableSetManagerTest` | Per-run dir layout + hard-link replication + cleanup |
| `DataGeneratorDeterminismTest` | Per-thread RNG sequence is deterministic for `(seed, threads)`; threads have distinct seeds (the Java-Random-48-bit-mask bug) |
| `ValidatorTest` | Synthetic UnfilteredRowIterator paths through PartitionHasher / PartitionComparator; `MismatchReport.formatForDisplay` shape |
| `PartitionHasherTest` | xxhash determinism + sensitivity to differences |
| `ErrataCheckerTest` | The matcher fires on the equal-ts-tiebreaker pattern and stays silent on real divergences (length mismatch, missing rows, timestamp diff) |

Stand-alone driver: `DataGeneratorDeterminismDriver` is a `main`-class
program (NOT a JUnit test) that prints inline determinism-check results
without needing the heavy test JVM setup. Run via:
```
java -cp build/classes/compaction-validator:build/tools/lib/compaction-validator.jar:...
     org.apache.cassandra.tools.compactionvalidator.data.DataGeneratorDeterminismDriver
```
Useful when validating seed-determinism changes since the JUnit path with
SnakeYAML / Cassandra bootstrap has a much higher cold-start cost.

---

## 17. Notable design decisions / gotchas

### Why per-thread byte quotas (datagen)
A shared `done` flag tied to a global `bytesWritten` AtomicLong races on
when each thread *observes* the flag — the same `(seed, threads, target)`
produced slightly different per-thread partition counts. Per-thread quotas
(`ceil(target / threads)`) eliminate the race; each thread runs to its own
deterministic stop point.

### Why SplitMix64 for thread seeds
The natural `dataSeed ^ (threadIndex << K)` mix with K ≥ 48 produces
identical `Random` state across every thread index because Java's
`Random(long)` constructor masks with `((1L << 48) - 1)`. The
`DataGeneratorDeterminismDriver` caught every thread producing the same
~1.4M partitions for thread indices 0–7. SplitMix64's finalizer spreads
variation across all 64 bits, so the low 48 (which `Random` uses) differ
between thread indices.

### Why parallel compaction tasks per side, not per backend
UCS returns batches of independent tasks per `getNextBackgroundTasks` call —
they operate on disjoint SSTable sets and are safe to run concurrently. The
shared `taskConcurrency` value is applied identically to both sides so the
cursor-vs-legacy speedup measurement reflects pipeline efficiency, not
asymmetric parallelism (running one side N=4 against the other N=1 would
let the more-parallel side win on wall-clock for reasons unrelated to the
algorithm).

### Why `cfs.getLiveSSTables().size()` for `sstablesOut`
`emitted.size()` accumulates every reader produced across all rounds.
Multi-pass UCS produces transient SSTables that get obsoleted; their
references stay in `emitted` but they're no longer on disk. For a 132-pass
run over 1.4M unique partitions, `emitted.size()` reports 264 sstables when
the actual final live count is 16.

### Why the validation panel uses dataGen partitions as denominator
Compaction's `partitionsProcessed` is cumulative across UCS multi-pass
rounds. A 132-pass run sums to ~185M partition-passes for a 1.4M-unique set.
Using that as the denominator made the validation bar read 4% when 8.1M
partitions had been checked (8.1M / 185M = 4.4%). The bar's denominator is
now `dataGenStats.getPartitionsWritten()` — the unique partition count.

### Why one parallel pool of 2 in `ParallelCompactor`, not pool-of-N
The pool runs the two `CompactionDriver` instances side-by-side (control +
experiment). Each driver internally runs its own `taskConcurrency`
parallel tasks. A single 2-thread pool here is correct — there's exactly
two top-level units of work.

### Phase A: why aggregate all shard results before picking canonical
With `validation_threads > 1`, multiple shards may find mismatches. The
"canonical" mismatch is the smallest-token result across all shards (= the
deterministic first divergence in scan order). Short-circuiting on the
first shard to return would non-deterministically pick whichever scheduler
happened to run first AND would skip the per-shard `partitionsChecked`
increments from the still-running shards — `ValidationStats` would
undercount.

### Why `Schema.instance.submit(...)` for keyspace + table registration
Direct `Schema.instance.load(ksm)` would update only the local schema view
but skip the cluster-metadata machinery. `submit` goes through
`ClusterMetadataService.commit` which is the supported offline path.
Deterministic `TableId.fromUUID(nameUUIDFromBytes(ks+"."+table))` lets the
orchestrator predict the on-disk path before constructing the CFS so source
SSTables can be hard-linked into it ahead of time (the CFS's id generator
otherwise scans the empty dir and starts at 1, colliding with later file
additions).

### Why hard links instead of copies for source → output dirs
A 10 GiB run × 3 dirs (source + 2 output) = 30 GiB of disk overhead with
copies; hard links share the same inode so the on-disk footprint stays at
10 GiB. Each per-side CFS can still mutate / obsolete its files
independently because compaction operates on file-name-keyed transactions —
the inode is shared but the directory entries are independent.

### Why Lanterna at 1 Hz (not 30/60)
Higher refresh rates caused visible flicker on most terminal emulators.
With the per-frame back-buffer wipe-to-spaces (§9), the diff-only
`screen.refresh()` produces no flicker at any rate — but 1 Hz is plenty
for human-readable progress (rates and bars don't need millisecond
precision). Animations that DID need higher rates (the LSM widget pulse,
sliding cursor) were dropped during the flicker fix.

### Why `RunOrchestrator` fires `onComparisonComplete` from inside each
### `ParallelCompactor` Callable, not after both finish
A side that finishes first must immediately freeze its rate metrics, or the
TUI keeps ticking elapsed time against a frozen partition count and the
displayed throughput visibly drifts toward zero. Each side's Callable fires
its own `onCompactionComplete(backend, stats)` the instant the driver
returns, before waiting on the other side.

### Why we don't try to vary `disk_access_mode` per side in this JVM
`DatabaseDescriptor.disk_access_mode` is process-global. The path readers
take (mmap vs standard ChannelProxy) is decided per-CFS at SSTableReader
open time but reads from the global value. To vary per side we'd need
either subprocess isolation (one JVM per side, shared input via files) or
intrusive Cassandra-internal refactoring. The first cut requires both sides
to specify the same `io_mode` — running the validator twice with different
values is the supported way to compare across modes.

---

## 18. Open issues / known gaps

- **Validation `partitionsChecked` exceeding unique count** — observed in a
  failing-seed report: 1.4M unique partitions, 8.1M `partitionsChecked` at
  ~4% bar. Disjoint-shards math says total should top out at 1.4M. The
  denominator fix (§9) made the bar percentage meaningful, but the
  numerator anomaly is not fully diagnosed. Suspect: `Bounds<PartitionPosition>`
  semantics around `minKeyBound`/`maxKeyBound` boundaries with the
  `(t+1).minKeyBound()` trick — needs per-shard counter instrumentation.
- **Per-side `disk_access_mode`** — currently parity-required. Subprocess
  isolation is the plausible path forward.
- **Validation-thread scaling** — 2.5 cores observed under
  `--validation-threads 18` on a real run. Per-shard cost may be small
  enough that scanner-open / merge-construction overhead dominates;
  diagnostics needed before further parallelism work.
- **TUI side-name labels in `RunHistoryPanel`** — the history panel shows
  the seed but not the side names. With matrix-style soak runs comparing
  many configurations, history rows ought to show a short config tag.
