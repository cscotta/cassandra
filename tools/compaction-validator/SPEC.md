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
    iterator-vs-cursor.yaml             baseline ITERATOR-vs-CURSOR comparison
    big-vs-bti.yaml                     ITERATOR(BIG) vs ITERATOR(BTI) cross-format comparison
    smoke-coverage.yaml                 small-target smoke run for the bug-coverage features
    smoke-big-vs-bti.yaml               small-target smoke run for the cross-format path
  plans/
    cursor-bug-coverage.md              design doc for the bug-1A-through-1B-through-4 coverage extensions
  src/
    org/apache/cassandra/db/compaction/
      PipelineSelector.java             enum + pipeline factory
      DirectCompactionRunner.java       same-package bridge into CompactionTask;
                                          accepts a fixed-nowInSec override
    org/apache/cassandra/tools/compactionvalidator/
      Main.java                         picocli entry
      RunLoop.java                      seed-progressing outer loop
      RunOrchestrator.java              one-run orchestration; pins nowInSec,
                                          runs FormatAuditor, classifies
                                          format violations, catches
                                          CorruptSSTableException
      RunResult.java                    immutable result record + Builder;
                                          carries schemaShape, gcGraceSeconds,
                                          formatViolations
      RunSeed.java                      SplitMix64-based seed progression
      SstableInfo.java                  POJO for events
      ProgressTap.java                  reporter interface
      PlainTextReporter.java            --no-ui sink
      compaction/
        CompactionDriver.java           drives one CFS through compaction;
                                          accepts pinned-nowInSec
        CompactionStats.java            mutable counters
        ParallelCompactor.java          runs control + experiment in parallel
                                          when their formats agree; serialises
                                          (toggling DatabaseDescriptor.set-
                                          SelectedSSTableFormat) when they
                                          differ. Captures pinned nowInSec
                                          shared across both sides.
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
      datagen/
        DataGenerator.java              parallel CQLSSTableWriter driver;
                                          multi-row partitions, TTL gen,
                                          power-law cells, wide-row clause
        DataGenStats.java               counters w/ monotonic estimate
        DataGeneratorDeterminismDriver.java   stand-alone determinism check
      logging/
        RunLogger.java                  append-only run log; emits Schema:,
                                          Errata:, Format: blocks
        SystemInfo.java                 startup-header system stats
      schema/
        SchemaGenerator.java            seed-driven schema generator;
                                          rolls SchemaShape and
                                          gc_grace_seconds
        GeneratedSchema.java            schema + per-side CQL builder;
                                          carries shape / nullFraction /
                                          gcGraceSeconds
        SchemaShape.java                NARROW vs WIDE shape tag
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
        Validator.java                  Phase A / depth checks / Phase B driver;
                                          combined parallel reverse-scan +
                                          point-read pass. Format-agnostic
                                          dispatch on BigTableReader /
                                          BtiTableReader; CorruptSSTableException
                                          handling.
        PartitionHasher.java            xxhash64 over Unfiltered stream
        PartitionComparator.java        cell-by-cell deep equals;
                                          keyspace-agnostic column equivalence
        MismatchReport.java             mismatch detail bundle
        ValidationStats.java            atomic counters + errata map
        ErrataRule.java                 known-bug catalogue (enum) with
                                          cell-pair / resurrected-row /
                                          resurrected-cell matchers and
                                          forFormatViolation Kind→Rule mapping
        ErrataChecker.java              rule-based partition matcher with
                                          merge-walk column iteration
        FormatAuditor.java              parallel byte-/semantic-level
                                          invariant audit on experiment SSTables
        FormatViolation.java            kind enum + violation record
  test/unit/
    org/apache/cassandra/db/compaction/
      DirectCompactionRunnerApiTest.java
    org/apache/cassandra/tools/compactionvalidator/
      compaction/SstableSetManagerTest.java
      datagen/DataGeneratorDeterminismTest.java
      schema/WideSchemaTest.java
      validation/ErrataCheckerTest.java
      validation/FormatAuditorTest.java
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
    format: BIG                            # BIG | BTI; null/omitted → JVM-global default
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
    format: BIG                            # CURSOR + BTI is rejected at config-load time
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
- **`format` choice**: `BIG` or `BTI` (case-insensitive). Resolved through
  `DatabaseDescriptor.getSSTableFormats()` so any registered format works
  uniformly; unknown names fail with the registered set in the error
  message. `null`/omitted → use the JVM-global default (BIG, set by
  `Main.bootstrapJvm`). The combination
  `pipeline: CURSOR + format: BTI` is rejected at config-load time:
  `CursorCompactor.unsupportedSchema` requires BIG output, so the
  combination would fail mid-run with a misleading "schema unsupported"
  message after the data has already been compacted. When the two sides
  agree on a format (or both omit it), compaction runs in parallel as
  before; when they differ, `ParallelCompactor` falls back to a serial
  implementation that toggles
  `DatabaseDescriptor.setSelectedSSTableFormat` around each side — the
  format selection is JVM-global at write time and there's no per-CFS
  override hook in `CompactionAwareWriter`. Source data (written by
  `DataGenerator`) is always emitted in whatever format is set at boot
  (BIG by default); the per-side `format` only affects each side's
  compaction *output*.
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
- Rolls a {@link SchemaShape}: 90% `NARROW`, 10% `WIDE`. Stored on the
  `GeneratedSchema` so the data generator and the run log can act on it.
- Per-shape column-count bands:
  - `NARROW`: 1–2 PK, 0–2 CK, 0–2 STATIC, 3–8 regular
  - `WIDE`: 1–2 PK, 0–2 CK, 0–32 STATIC, 64–200 regular
- Clustering keys get random ASC/DESC; if any STATIC columns are picked
  but ckCount==0, statics drop (CQL doesn't allow them).
- Type catalogue:
  PKs from `{int, bigint, uuid, ascii, text}`, regulars from
  `{int, bigint, uuid, ascii, text, timestamp, boolean, float, double, blob,
    varint, decimal, frozen<set<text>>, frozen<list<int>>,
    frozen<map<text,int>>, tuple<int,text>}`
  (collections frozen because `CursorCompactor.unsupportedSchema` rejects
  non-frozen / "complex" columns).
- UCS defaults: `target_sstable_size` ∈ {64, 128, 256, 512} MiB,
  `base_shard_count` ∈ {1, 2, 4}; LZ4 chunk size ∈ {4, 16, 64, 256, 1024} KiB.
- **Per-run NULL fraction** rolled into the schema: `NARROW` from
  `[0.05, 0.50]`, `WIDE` from `[0.70, 0.95]`. Wide-shape values are tuned
  high so cursor compaction's column-subset encoding path actually fires
  on the rows the data generator emits — it only kicks in when the row's
  cell count is much smaller than the regular-column count.
- **`gc_grace_seconds` rotation** sampled per seed so soak runs exercise
  the tombstone-purge code path some of the time:
  - 40% `0` — aggressive purge, every tombstone eligible during compaction
  - 30% `3 600` — 1 hour, GC machinery active but nothing in our run window
    is purgeable
  - 20% `86 400` — 1 day
  - 10% `864 000` — Cassandra default (no purge in any reasonable run window)
  Both pipelines see the same option, so a divergence in their purge
  decisions is necessarily a cursor-vs-iterator bug.
- Keyspace name `cvtest_<low32(seed)>`, table name `t`, deterministic
  `TableId` derived from `keyspace.tableName` for predictable disk paths.
- Returns `GeneratedSchema` with column lists, `defaultCompaction` /
  `defaultCompression` specs, the rolled `shape`, `nullFraction`, and
  `gcGraceSeconds`.

`GeneratedSchema.buildCqlForSide(keyspaceOverride, side)` overlays the side's
specs on the defaults and emits `CREATE TABLE ... WITH compaction = {...}
AND compression = {...} AND gc_grace_seconds = N [AND CLUSTERING ORDER BY ...]`.

### Phase 2 — data generation
`DataGenerator` runs `datagen_threads` parallel `CQLSSTableWriter` instances,
each with its own thread seed (`SeedUtil.threadDataSeed(dataSeed, i)`) and
its own per-thread byte quota (`ceil(target / threads)`). Per-thread quotas
guarantee determinism — an earlier shared `done` flag tied to a global byte
counter raced on `AtomicLong` updates from per-thread flush listeners and
made the same `(seed, threads)` produce slightly different per-thread
partition counts.

The INSERT statement always carries a trailing `USING TTL ?` bind so the
writer can choose per row whether to apply a TTL (TTL=0 maps to "no TTL"
in Cassandra; the cell is written without `IS_EXPIRING_MASK`).

**Partition shape — depends on whether the schema has clustering keys:**
- `ckCount >= 1` → multi-row partitions. The PK is generated *once* per
  outer iteration via `generatePartitionKey(...)` and reused across
  `rowsPerPartition` rows that each get fresh CK / static / regular cells
  via `buildRowValuesWithTtlSharingPk(...)`. `recordPartition()` fires
  once per outer iteration. This is the partition shape Cassandra's
  format actually expects: many rows, single partition key.
- `ckCount == 0` → each row IS its own partition (CQL semantics: multiple
  writes with the same PK on a CK-less table merge into one row, so emitting
  multi-row "partitions" with the same PK would just be redundant
  overwrites). Each row generates a fresh PK and bumps `recordPartition()`
  per row, so `partitionsWritten` matches the actual unique partition
  count the validator will see post-compaction.

Per-thread RNG draws (deterministic given the seed):
1. `maxSstableMib` (range `[1, 512]`)
2. Loop:
   - **Wide-row roll**: with probability 5%, `rowsPerPartition` is drawn
     from `[100, 1000]`; otherwise `[1, 16]`. Wide rows + occasional large
     cells (below) drive partition size past the 64 KiB Index.db block
     boundary that compaction bug 3 needs.
   - For each row in the partition: build values via
     `buildRowValuesWithTtl[SharingPk]`, append a TTL bind, call
     `writer.addRow(values)`.

**Per-cell value choice (`pickColumnValue`)**:
- `NARROW` shape: each non-PK / non-CK column rolls live with probability
  `1 - schema.nullFraction`, otherwise NULL (which CQL serialises as a
  per-cell tombstone).
- `WIDE` shape: each non-PK / non-CK column rolls
  - `ByteBufferUtil.UNSET_BYTE_BUFFER` (the column is OMITTED — no cell
    written, exercises cursor compaction's column-subset encoding path)
    with probability `nullFraction`,
  - NULL (tombstone) with a fixed 10% probability of the remainder,
  - a typed live value otherwise.

**Per-cell size distribution** — bumped from the original 256-byte cap so
some partitions cross 64 KiB:
- `blob`: 99% drawn from `[1, 256]` bytes, 1% drawn from `[1024, 1 048 576]`
  (1 MiB max).
- `text` / `ascii`: 99% drawn from `[1, 64]` chars, 1% from `[256, 16 384]`.
- Numeric / boolean / uuid columns are unchanged.

**TTL distribution** — appended once per row:
- 90% TTL = 0 (no expiry).
- 10% TTL drawn from `[7 days, 30 days]`. Long enough that no cell can
  expire during a single validator run regardless of how long compaction or
  validation takes — avoids the false-positive class where the cursor and
  iterator pipelines might otherwise evaluate expiry at slightly different
  wall-clock times. The bug-1B trigger only depends on cells carrying the
  `IS_EXPIRING_MASK` flag, not on their actually expiring.

`writer.addRow(values)` → `CQLSSTableWriter` buffers and flushes when the
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
`ParallelCompactor` runs two `CompactionDriver` instances concurrently when
both sides agree on the SSTable output format (or both omit it), one per
side. Each fires `reporter.onCompactionComplete(backend, stats)` the
instant its driver returns so a side that finishes first immediately freezes
its rate metrics (avoids the rate counter visibly drifting toward zero while
the other side is still going).

**Cross-format runs:** `ParallelCompactor.run()` checks whether the two
sides specify compatible formats. When the formats differ (e.g. `control:
BIG` / `experiment: BTI`), it dispatches to a serial implementation
instead — `DatabaseDescriptor.setSelectedSSTableFormat` is JVM-global at
write time, and `ColumnFamilyStore.newSSTableDescriptor(directory)` reads
that global, so two compaction threads writing concurrently with
different formats would race on the global. The serial path runs the
sides one after another and toggles the global between them. Both sides
still share a single `pinnedNowInSec` (captured before either starts) so
that GCGS=0 runs don't spuriously diverge whenever the two compactions
straddle a wall-clock-second boundary. Cross-format runs lose
parallelism (~2× wall-clock for the compaction phase) but produce
correct results; same-format runs are unchanged.

**Pinned `nowInSec`:** `ParallelCompactor.run()` captures one
`FBUtilities.nowInSeconds()` *before* dispatching the two drivers and
threads it through `CompactionDriver` → `DirectCompactionRunner`. Both
sides therefore compute the same `gcBefore = nowInSec - gcGraceSeconds`
and make identical tombstone-purge / TTL-expiry decisions. Without this,
a 1–2 second drift between when the two threads enter their pipeline
loop could let one side drop a near-expiry cell that the other still
considered live — a spurious "divergence" that's actually just clock
noise. With the pin, GCGS rotation (Phase 1) becomes a clean correctness
test: both pipelines see identical inputs *and* identical purge time.

`CompactionDriver`:
- Accepts a `taskConcurrency` per backend (config `compaction_threads`)
  and an optional `fixedNowInSec` override propagated from
  `ParallelCompactor`.
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
- Uses `fixedNowInSecOverride` when set (instead of reading
  `FBUtilities.nowInSeconds()` at compaction-start time) so both sides of
  an A/B comparison see the same `nowInSec`
- Periodically (every ~1MB) invokes `ProgressCallback.onProgress(...)` and
  `onSStableEmitted(...)` so the TUI gets live throughput

`PipelineSelector.Backend` enum: `ITERATOR`, `CURSOR`. `create(...)` selects
between `IteratorCompactionPipeline` and `CursorCompactionPipeline`,
bypassing the global `cursorCompactionEnabled` flag.

### Phase 4b — format-level invariant audit
`FormatAuditor.audit(experimentSSTables, clusteringComparator)` walks each
experiment-side SSTable in parallel (up to `availableProcessors()` workers,
one task per SSTable) and records `FormatViolation`s on cells/rows that
violate well-formedness invariants the standard SSTable reader is too
tolerant to surface. Synchronised result list, no shared writer state past
the appended violations.

The control side is the format reference and is never audited — any
divergence-relative-to-itself would be a different (more serious) class of
bug than the cursor-vs-iterator validation this tool is built for.

**Invariants checked** (per cell, except where noted):

- `FLAG_DELETE_AND_EXPIRE` — an "expiring" cell whose `localDeletionTime`
  doesn't satisfy `(timestamp / 1_000_000) + ttl` (within ±2 seconds).
  This is the read-side fingerprint of compaction bug 1A: the writer set
  both `IS_DELETED_MASK` and `IS_EXPIRING_MASK` in the on-disk flags
  byte; the deserialiser's `else if` chain takes the IS_EXPIRING branch
  and reads `(ttl, ldt)` where the value on disk was the IS_DELETED-style
  deletion time, not `writetime + ttl`.
- `ROW_OUT_OF_ORDER` — strict reverse (`prev > curr`) clustering ordering
  within a partition. A real writer-ordering bug; *not* mapped to any
  cascade rule.
- `DUPLICATE_CLUSTERING` — equal (`prev == curr`) clustering ordering
  within a partition. Structurally invalid, but in practice almost always
  the downstream symptom of bug 1A: once the deserialiser drifts past a
  misaligned flag-byte cell, garbage bytes can parse as a "row marker"
  with the same clustering as the previous row.
- `TIMESTAMP_NEGATIVE` / `TTL_NEGATIVE` — the deserialiser's `vint`
  decode underflows the long range from misaligned bytes, surfacing
  wildly negative values that no `CQLSSTableWriter` codepath produces.
  Both are downstream cascade symptoms of bug 1A.
- `SCANNER_FAILURE` — wrapper kind for the `CorruptSSTableException` (or
  any other `Throwable`) the deserialiser throws when it eventually hits
  bounds it can't satisfy. Catches the terminal point of the bug-1A
  cascade.

`RunOrchestrator` collects the full violation list into `RunResult`,
classifies each into "suppressed by an active errata" vs "unsuppressed" via
`ErrataRule.forFormatViolation(kind)`, and folds the suppression decision
into the standard pass/fail flow (§8). The `Validator` (§5 Phase 5) is
also wrapped in a `try/catch (CorruptSSTableException)` that converts the
exception to a `SCANNER_FAILURE` violation rather than letting it abort the
run before suppression is consulted.

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
- **Phase Depth-Checks (parallel, sampled)**: only runs if Phase A passed.
  A *single combined* forward walk per shard (sharded across the same
  `validation_threads` token ranges as Phase A) does both:
  - **Reverse-iter hash** — opens a per-partition reversed
    `UnfilteredRowIterator` (via {@code BigTableReader.rowIterator(key, Slices.ALL,
    ColumnFilter.all, reversed=true, ...)} or its {@code BtiTableReader}
    counterpart, dispatched by `instanceof` since the API isn't
    abstract on the common parent), merges across SSTables, hashes
    the reverse stream, and compares. Catches compaction bug 2's broken
    `prevUnfilteredSize` (forward bytes are correct, reverse seek lands in
    the wrong place).
  - **Indexed point-read** — for partitions whose schema has clustering
    keys, captures the partition's last clustering during the forward walk,
    then issues a point read via the same format-agnostic dispatch on
    each side and compares the slice hash. Catches compaction bug 3's
    off-by-one in the Index.db final-block width.
  Sampled at one in 100 partitions per shard (`REVERSE_SCAN_STRIDE` /
  `POINT_READ_STRIDE`). Bug 2 affects every reverse iteration uniformly so
  1% sampling still catches it essentially every run; bug 3 only fires on
  partitions ≥ 64 KiB and the data generator's wide-row + power-law-cell
  knobs produce enough of those per 1 GiB run that 1% sampling reliably
  hits multiple. The combined forward walk avoids re-iterating the
  partition list twice; the sharding lets the pass saturate cores instead
  of running serially on the orchestrator's thread.
- **Phase B (deep compare)**: re-opens both sides' scanners restricted to
  the offending partition's bounds and runs `PartitionComparator.compare`,
  producing a `MismatchReport` that walks partition key → partition-level
  deletion → static row → body unfiltereds → row clustering / liveness /
  deletion / cells (column metadata, value bytes, timestamp, TTL,
  localDeletionTime, counter flag, complex path). First difference wins.

  Column equality inside `PartitionComparator.compareColumnData` uses a
  keyspace-agnostic check (name + kind + position + CQL3 type) rather
  than `ColumnMetadata.equals` — the two sides come from independent
  per-side keyspaces, so the strict equals would always return false and
  produce a misleading "column metadata differs" error before the
  cell-value check ran. This mirrors `ErrataChecker.sameColumn`'s
  workaround.

`ValidationStats`:
- `partitionsChecked`, `rowsChecked`, `partitionMismatches` — `AtomicLong`
  (concurrent shard updates)
- `errataOccurrences: EnumMap<ErrataRule, AtomicLong>` — pre-populated for
  all rules, incremented from the Validator's hash-mismatch path when
  `ErrataChecker.matches(...)` fires (§8) and from `RunOrchestrator`'s
  format-violation classification path (Phase 4b)
- `durationMs` — single writer (Validator's outer block), volatile long
- `hasMismatches()` returns `true` only when `partitionMismatches > 0` —
  errata-suppressed partitions don't count.

### Phase 6 — result assembly + cleanup
- `RunResult.Builder` is populated through every phase; the orchestrator
  builds the final immutable `RunResult` and fires `reporter.onRunComplete`
  or `onRunFailed` (with the `MismatchReport`).
- `RunResult` carries the schema's rolled `shape` (`NARROW` / `WIDE`) and
  `gcGraceSeconds` so post-mortem analysis can correlate failures with
  the structural / GC dimensions of the run, plus the
  `formatViolations` list from Phase 4b for the same reason.
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
- Non-`BigFormat` output SSTables → `Main.bootstrapJvm` calls
  `setSelectedSSTableFormat(BigFormat.getInstance())` to set the JVM-wide
  default to BIG. A side that pins `format: BTI` in YAML works fine when
  paired with `pipeline: ITERATOR` (the iterator pipeline supports both
  formats), but `pipeline: CURSOR + format: BTI` is rejected at
  config-load time by `RunOrchestrator.resolveFormat` rather than
  letting the run reach `CursorCompactor.isSupported` with a
  misleading error.
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
Any new divergence still halts loudly. The same suppression set also
governs whether `FormatAuditor` violations from Phase 4b cause failure.

### Active rules

| `cliName()` | `ErrataRule` | Bug |
|---|---|---|
| `equal-ts-tiebreaker` | `EQUAL_TIMESTAMP_TIEBREAKER` | Cursor's `mergeCells` `COMPARE` branch inverts `compareUnsigned` — picks smaller value bytes when two cells share `(ts, ttl, ldt, counter-flag)` but differ in value. |
| `tombstone-expiring-flags-both-set` | `TOMBSTONE_EXPIRING_FLAGS_BOTH_SET` | Bug 1A — cursor sets BOTH `IS_DELETED_MASK` and `IS_EXPIRING_MASK` in the on-disk flags byte. Detected via the `FormatAuditor`'s expiration-formula invariant. |
| `tombstone-resurrected-by-expiring` | `TOMBSTONE_RESURRECTED_BY_EXPIRING` | Bug 1B — at equal timestamp, cursor wins for an expiring cell where `Cells.resolveRegular` requires the tombstone to win (silently resurrects deleted data). |
| `prev-unfiltered-size-zero` | `PREV_UNFILTERED_SIZE_ZERO` | Bug 2 — cursor always writes `prevUnfilteredSize=0`. Caught by the depth-checks pass's reverse-iter hash. |
| `index-block-width-off-by-one` | `INDEX_BLOCK_WIDTH_OFF_BY_ONE` | Bug 3 — cursor's `Index.db` final-block width is off by one for partitions ≥ 64 KiB. Caught by the depth-checks pass's indexed point-read. |
| `wide-column-subset-dropped-last` | `WIDE_COLUMN_SUBSET_DROPPED_LAST` | Bug 4 — cursor's column-subset row encoder drops the highest-position cell. Reserved for soak-loop suppression; no per-cell matcher today (Phase B's row diff catches it directly). |

### Matcher methods on `ErrataRule`

- `explainsCellValueDifference(legacy, cursor)` — narrow value-only check.
  Default false. Used by `equal-ts-tiebreaker` whose entire signature is
  "everything matches except the value bytes."
- `explainsCellPairDifference(legacy, cursor)` — broad multi-field check.
  Consulted *before* the value-only path so a rule that spans multiple
  fields (e.g. bug 1B's tombstone-vs-expiring with different ttl, ldt,
  AND value) can suppress without being disqualified by the
  "non-value field also differs" guard. Used by
  `tombstone-resurrected-by-expiring` and
  `tombstone-expiring-flags-both-set`.
- `explainsResurrectedRow(cursorRow)` — row-level asymmetry: legacy's row
  collapsed to empty (e.g. via `Row.Merger` when same-TS tombstones
  reconcile away), cursor's row still carries cells. Used by
  `tombstone-resurrected-by-expiring`'s static-cell variant — fires
  when the cursor row contains only expiring + tombstone cells with at
  least one expiring.
- `explainsResurrectedCell(cursorCell)` — column-within-row asymmetry:
  cursor has an extra cell for a column legacy doesn't carry in this
  row (cursor's bug-1B kept an expiring cell that legacy's compaction
  correctly dropped). Used by `tombstone-resurrected-by-expiring`'s
  column-extra variant — fires when the extra cell is expiring.

### Format-violation → rule mapping (`forFormatViolation`)

The `FormatAuditor` produces violations whose kinds correspond to specific
checkpoints in compaction bug 1A's cascade. `ErrataRule.forFormatViolation(kind)`
maps each suppressible kind to `TOMBSTONE_EXPIRING_FLAGS_BOTH_SET` so a
soak run with that rule active rides through the whole cascade rather than
halting on each downstream symptom:

| `FormatViolation.Kind` | Maps to | Comment |
|---|---|---|
| `FLAG_DELETE_AND_EXPIRE` | `TOMBSTONE_EXPIRING_FLAGS_BOTH_SET` | Direct cell-level signature. |
| `DUPLICATE_CLUSTERING` | `TOMBSTONE_EXPIRING_FLAGS_BOTH_SET` | Cascade: deserialiser reads garbage as a row marker with the same clustering as the previous row. |
| `TIMESTAMP_NEGATIVE` | `TOMBSTONE_EXPIRING_FLAGS_BOTH_SET` | Cascade: vint timestamp reads from misaligned bytes underflow. |
| `TTL_NEGATIVE` | `TOMBSTONE_EXPIRING_FLAGS_BOTH_SET` | Cascade: same idea on the TTL slot. |
| `SCANNER_FAILURE` | `TOMBSTONE_EXPIRING_FLAGS_BOTH_SET` | Cascade terminus: deserialiser hits bounds checks and throws. |
| `ROW_OUT_OF_ORDER` | *unmapped* | Strict reverse (`prev > curr`) — would indicate a real ordering bug outside the bug-1A cascade. Always fails. |

### Architecture

- `ErrataRule` enum — each entry has `cliName()`, `description()`, and
  per-rule overrides of the matcher methods listed above. CLI parsing via
  `ErrataRule.parseCliList(commaSeparated)` rejects unknown names with the
  full known-rule list in the error message.
- `ErrataChecker` — given a partition pair, walks both sides in lockstep
  (mirroring `PartitionComparator`'s structure) and asks each active rule
  whether each per-cell, per-row, or row-asymmetry difference fits its
  predicates. The walk uses a *merge-walk* by column name (not lockstep)
  so cursor-extra columns can be consulted via
  `explainsResurrectedCell` without bailing on the first column-metadata
  mismatch. The body iteration tail also handles cursor-has-more-rows
  asymmetry via `explainsResurrectedRow`. Static-row asymmetry
  (legacy empty / cursor non-empty) goes through the same per-row
  matcher. If EVERY difference is covered by SOME rule, returns the set
  of fired rules; otherwise returns the empty set → caller treats as a
  real mismatch.
- Column equality inside `ErrataChecker.sameColumn` uses a
  name+kind+position check, NOT `ColumnMetadata.equals`, because the
  two sides come from different keyspaces (different `ksName`) and the
  default equals would never agree. `PartitionComparator.compareColumnData`
  uses the same workaround to avoid producing misleading "column metadata
  differs: control=c3 experiment=c3" failures.
- `Validator.checkErrataForPartition(key)` — re-scans just that partition
  with bounds-restricted scanners (cheap), runs the matcher, and returns
  the rule set.
- `RunOrchestrator` classifies `FormatAuditor` violations into "suppressed
  by an active errata" vs "unsuppressed" using
  `forFormatViolation(kind)`; the unsuppressed list is treated as a hard
  failure (with a synthetic `MismatchReport` pointing at the first
  violation) when Phase A passes but the auditor saw something. The
  validator's `validate()` is wrapped in `try/catch (CorruptSSTableException)`
  that converts the throwable to a `SCANNER_FAILURE` violation and
  re-enters the suppression decision rather than aborting the run.
- `ValidationStats.errataOccurrences` — `EnumMap<ErrataRule, AtomicLong>`
  pre-populated for all rules. Multiple shards bump counters via
  `incrementAndGet` without locking.
- Reports show errata loudly:
  - `PlainTextReporter.onValidationComplete` prints a Unicode-bordered
    block listing each rule + count when total > 0.
  - `RunLogger` writes an `Errata: N suppressed (run still PASS):` block
    with per-rule counts, and a `Format: N violation(s)` block listing
    auditor findings (suppressed and unsuppressed alike) for post-mortem.

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
  Schema:  cvtest_deadbeef.t  (2 PK, 1 CK, 2 static, 5 regular cols, shape=NARROW, gc_grace=3600s)
  UCS:     target_sstable_size=256MiB  base_shard_count=2  compression_chunk=16KiB
  Schema CQL:
    CREATE TABLE cvtest_deadbeef.t (
      ...
    ) WITH compaction = {...} AND compression = {...} AND gc_grace_seconds = 3600;
  DataGen: 10.3 GiB  |  1.4M partitions  |  52M rows  |  310 MiB/s avg
  legacy:    18.4s  |  290 MiB/s  |  1.4M partitions/s
  cursor:    15.7s  |  340 MiB/s  |  1.6M partitions/s  |  Speedup: 1.17x
  Valid:   PASS  |  1.4M partitions matched  |  52M rows matched
  Errata:  3 suppressed (run still PASS):
    equal-ts-tiebreaker             3
  Format:  2 violation(s) on experiment side:
    flag-delete-and-expire          1
    scanner-failure                 1
    [flag-delete-and-expire] sstable=pa-12-big-Data.db pk=...
    [scanner-failure] sstable=pa-12-big-Data.db pk=(scan failed) — scanner threw CorruptSSTableException: ...
```

The `Schema:` line includes the schema's rolled `shape` (`NARROW` or
`WIDE`) and effective `gc_grace_seconds` so post-mortem analysis can
correlate failures with structural / GC dimensions of the run. The
`Format:` block is emitted whenever `FormatAuditor` reported anything
(suppressed or not); per-kind counts plus the first ~32 violation
descriptions land in the log so a grep across a long-running soak can
quickly surface "are we still seeing this known bug?".

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
5. `setSelectedSSTableFormat(BigFormat.getInstance())` — the default for
   data-gen and (when no per-side `format:` is set) for both compactions.
   Cursor only supports BIG; iterator supports both. Per-side `format:`
   overrides this temporarily for the side's compaction-output writer
   only — see §5 Phase 4 for the parallel-vs-serial dispatch.
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
| `WideSchemaTest` | `SchemaShape` rotation hits all four GCGS values + WIDE roll probability sits in the expected band; wide schemas have ≥64 regular columns and high NULL fraction; CQL contains `gc_grace_seconds = N` |
| `ValidatorTest` | Synthetic UnfilteredRowIterator paths through PartitionHasher / PartitionComparator; `MismatchReport.formatForDisplay` shape |
| `PartitionHasherTest` | xxhash determinism + sensitivity to differences |
| `FormatAuditorTest` | Each invariant check fires on the synthetic violation and stays silent on the well-formed case (`FLAG_DELETE_AND_EXPIRE`, `ROW_OUT_OF_ORDER`, `DUPLICATE_CLUSTERING`, `TIMESTAMP_NEGATIVE`, well-formed expiring cell, expiration-tolerance edge case) |
| `ErrataCheckerTest` | The cell-pair matcher fires on the equal-ts-tiebreaker pattern, the bug-1B tombstone-vs-expiring pattern, and the bug-1A broken-expiration-formula pattern; stays silent on real divergences (length mismatch, missing rows, timestamp diff). Also covers the row-asymmetry path (legacy empty / cursor non-empty), the column-extra path (cursor has an extra expiring column), and the `forFormatViolation` Kind→Rule mapping. |

Stand-alone driver: `DataGeneratorDeterminismDriver` is a `main`-class
program (NOT a JUnit test) that prints inline determinism-check results
without needing the heavy test JVM setup. Run via:
```
java -cp build/classes/compaction-validator:build/tools/lib/compaction-validator.jar:...
     org.apache.cassandra.tools.compactionvalidator.datagen.DataGeneratorDeterminismDriver
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

- **`partitionsChecked` count alignment** — fixed by the `DataGenerator`
  multi-row-partition rework. Validator's `partitionsChecked` and
  `dataGen.partitionsWritten` now agree post-compaction, so the TUI bar
  reaches 100% at end of validation rather than topping out at a few
  percent of a vastly-too-large denominator.
- **Per-side `disk_access_mode`** — currently parity-required. Subprocess
  isolation is the plausible path forward.
- **Range-tombstone / partition-tombstone generation** — the
  `DataGenerator` emits cell tombstones (via `INSERT NULL`) but no
  `DELETE FROM t WHERE pk=?` (partition tombstone) or
  `DELETE FROM t WHERE pk=? AND ck >= ?` (range tombstone). With the
  GCGS rotation in place, those code paths in cursor compaction are
  technically reachable but currently unexercised. Worth adding as a
  follow-on so the range-tombstone reconciliation path gets coverage.
- **`PREV_UNFILTERED_SIZE_ZERO` direct field check** — bug 2 is caught
  via the depth-checks pass's reverse-iter hash divergence. A
  byte-level check on the experiment side that walks `Data.db` and
  asserts `prevUnfilteredSize > 0` directly would fire even if the
  reverse-iter sample missed the affected partition (we sample 1-in-100).
- **`INDEX_BLOCK_WIDTH_OFF_BY_ONE` direct field check** — same idea for
  bug 3: the indexed point-read sample catches it, a byte-level
  comparison of `Index.db` final-block width against the
  `Data.db`-derived extent would catch it on every affected partition.
- **`WIDE_COLUMN_SUBSET_DROPPED_LAST` cell matcher** — the rule today
  is a suppression marker. The Phase B row diff catches the pattern
  (legacy has the cell, cursor doesn't, on a wide-shape row), but
  the rule has no narrow per-cell predicate yet. Adding one would let
  the matcher count occurrences distinctly from generic
  `tombstone-resurrected-by-expiring`.
- **Strict-reverse `ROW_OUT_OF_ORDER`** — the only `FormatViolation`
  kind that always fails the run (no errata maps to it). If a real
  cursor bug ever produces `prev > curr` clusterings as part of the
  bug-1A cascade, this rule would need a guard like the others — but
  we haven't observed that in the wild.
- **TUI side-name labels in `RunHistoryPanel`** — the history panel shows
  the seed but not the side names. With matrix-style soak runs comparing
  many configurations, history rows ought to show a short config tag.
