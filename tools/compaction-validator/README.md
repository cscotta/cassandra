# compaction-validator

A standalone tool that runs the same generated dataset through two
independently-configured compaction pipelines and asserts the results are
**byte- / cell-identical**. Originally built to validate cursor-based
compaction against the legacy iterator pipeline, it has been generalised
into an A/B framework that compares any two compaction configurations.

The tool generates a deterministic dataset from a 64-bit seed, hard-links
the source SSTables into two output directories, compacts each side under
its own configuration, and walks both outputs in lockstep — partition by
partition, cell by cell — to verify they're identical. Mismatches surface
with a full structural diff plus a one-line re-run command, and the
preserved input + output set lets the user reproduce the failure
deterministically.

A live Lanterna TUI shows progress for every phase (schema generation,
data generation, parallel compaction, validation), with a continuously
appended log file and a recent-runs history panel.

For the full architectural / build-from-scratch reference, see
[`SPEC.md`](../../SPEC.md) at the repo root.

---

## Table of contents

- [What it tests](#what-it-tests)
- [What it does NOT test (yet)](#what-it-does-not-test-yet)
- [Quick start](#quick-start)
- [Building](#building)
- [Running](#running)
  - [Command-line flags](#command-line-flags)
  - [Environment variables](#environment-variables)
- [YAML configuration reference](#yaml-configuration-reference)
- [Example configurations](#example-configurations)
  - [1. Legacy iterator vs cursor (the original use case)](#1-legacy-iterator-vs-cursor-the-original-use-case)
  - [2. UCS vs STCS](#2-ucs-vs-stcs)
  - [3. LZ4 vs Zstd compression](#3-lz4-vs-zstd-compression)
  - [4. UCS shard-count comparison (cursor vs cursor)](#4-ucs-shard-count-comparison-cursor-vs-cursor)
  - [5. Compression chunk size comparison](#5-compression-chunk-size-comparison)
  - [6. Long soak with known-bug suppression](#6-long-soak-with-known-bug-suppression)
  - [7. Quick smoke test (CI-friendly)](#7-quick-smoke-test-ci-friendly)
  - [8. Reproducing a known failure](#8-reproducing-a-known-failure)
- [Output and reporting](#output-and-reporting)
- [How it works (high level)](#how-it-works-high-level)
- [Determinism guarantees](#determinism-guarantees)
- [Errata system](#errata-system)
- [Resource expectations](#resource-expectations)
- [Troubleshooting](#troubleshooting)
- [Project layout](#project-layout)
- [Adding a new errata rule](#adding-a-new-errata-rule)
- [Running the tests](#running-the-tests)
- [Future work](#future-work)

---

## What it tests

For a given seed and YAML config, the validator verifies that two
compaction configurations applied to the **same source data** produce
**byte- / cell-identical merged output**. Identity is checked via four
independent passes:

1. **Phase A — token-range parallel hash sweep.** The Murmur3 ring is
   split into N shards; each worker walks both sides in lockstep,
   computing a deterministic hash over each partition's full
   `Unfiltered` stream and comparing.
2. **Phase Depth-Checks — combined parallel reverse-scan + indexed
   point-read.** Sharded across the same N workers as Phase A. For each
   sampled partition (1-in-100 by default):
   - Open per-partition reversed iterators on both sides, hash the
     reverse stream, and compare. Catches divergences that Phase A
     misses because the forward bytes are correct but reverse seek
     navigates differently — the signature of compaction bug 2's
     broken `prevUnfilteredSize`.
   - For partitions whose schema has clustering keys, issue a slice
     point-read on the partition's last clustering on each side and
     compare. Catches divergences that only manifest when the reader
     seeks into a specific Index.db block — the signature of compaction
     bug 3's off-by-one final-block width.
3. **`FormatAuditor` byte-/semantic-level invariant audit** on the
   experiment-side SSTables, parallelised across `availableProcessors()`
   workers. Asserts cell-flag mutual exclusivity (catches compaction
   bug 1A — `IS_DELETED` and `IS_EXPIRING` set in the same flags byte
   via the read-side fingerprint that `localDeletionTime != writetime + ttl`),
   row clustering ordering, and timestamp / TTL non-negativity. Reports
   `CorruptSSTableException`s under their own kind (the cascade terminus
   of bug 1A) so soak runs can suppress them as known symptoms.
4. **Phase B — cell-by-cell deep equals.** When Phase A or the depth
   checks flag a mismatch, both sides are re-scanned restricted to the
   offending partition and compared field by field: clustering,
   primary-key liveness, row-level deletion, every cell's `(column, value
   bytes, timestamp, ttl, localDeletionTime, counter flag, complex path)`.

The framework can vary any of:

- **Compaction pipeline** — `ITERATOR` (legacy) vs `CURSOR` (the original
  motivating comparison).
- **SSTable output format** — `BIG` (the historical format,
  `pa-N-big-Data.db` + `Index.db`) vs `BTI` (the trie-indexed format,
  `ea-N-bti-Data.db` + `Partitions.db` + `Rows.db`). Specified per side
  via `format: BIG | BTI` in the YAML; `null` / omitted ⇒ inherit the
  JVM-global default (BIG, set in `Main.bootstrapJvm`). Cursor compaction
  only supports BIG output, so `pipeline: CURSOR + format: BTI` is
  rejected at config-load time. When the two sides specify different
  formats, the compaction phase runs the two sides serially (toggling
  `DatabaseDescriptor.setSelectedSSTableFormat` between them) instead of
  in parallel — see [How it works](#how-it-works-high-level).
- **Compaction strategy** — `UnifiedCompactionStrategy`,
  `SizeTieredCompactionStrategy`, `LeveledCompactionStrategy`,
  `TimeWindowCompactionStrategy`, with their full per-strategy options.
- **Compression codec** — `LZ4Compressor`, `ZstdCompressor`,
  `SnappyCompressor`, `DeflateCompressor`, with codec-specific options
  (chunk size, compression level, ...).
- **`disk_access_mode`** — pinned in the config but with the constraint
  that both sides must use the same value (see
  [Future work](#future-work)).

For each run the tool generates a random Cassandra schema (column count,
types including frozen collections / tuples, clustering ASC/DESC, static
columns, statics-with-CKs constraint, etc.) and a deterministic dataset.
Several schema- and data-shape knobs are seed-rolled per run so the soak
loop covers a wide range of cursor-compaction code paths:

- **`SchemaShape` rotation** — 90% `NARROW` (3-8 regular columns,
  per-cell NULL fraction in `[0.05, 0.50]`), 10% `WIDE` (64-200 regular
  columns, NULL/omit fraction in `[0.70, 0.95]`). Wide shapes write
  rows that genuinely *omit* most columns via
  `ByteBufferUtil.UNSET_BYTE_BUFFER` — required to exercise cursor's
  column-subset row encoder (compaction bug 4's surface).
- **`gc_grace_seconds` rotation** — 40% `0`, 30% `3 600`, 20% `86 400`,
  10% `864 000` (default 10 days). At GCGS=0 every tombstone is purge-
  eligible during compaction, so the tombstone-purge code path
  (`CompactionController.getFullyExpiredSSTables`, per-cell purge
  during merge) is exercised on most runs and both pipelines must agree
  on the resulting purge decisions.
- **TTL generation** — 10% of rows carry a row-level TTL drawn from
  `[7 days, 30 days]` so cells carry the `IS_EXPIRING_MASK` flag (the
  bug-1A trigger) without ever expiring during a single run regardless
  of how long compaction or validation takes — long TTLs avoid the
  false-positive class where cursor and iterator pipelines might
  otherwise evaluate expiry at slightly different wall-clock times. As
  defence in depth, `ParallelCompactor` also pins one
  `FBUtilities.nowInSeconds()` and threads it through both sides so
  they compute identical `gcBefore`.
- **Multi-row partitions** — when the schema has clustering keys, each
  outer write loop generates the PK once and writes
  `rowsPerPartition` rows that share it. (For 0-CK schemas, each row is
  structurally its own partition, so the data generator falls back to
  fresh-PK-per-row.) This is what makes the wide-row probability and
  the power-law cell-size distribution actually meaningful: with the
  earlier "fresh PK per row" behaviour every partition was single-row
  and never crossed the 64 KiB Index.db block boundary that bug 3
  needs.
- **Wide-row clause** — 5% of partitions roll `rowsPerPartition` from
  `[100, 1000]` instead of `[1, 16]`. Combined with the cell-size
  power-law (1% of `blob` cells up to 1 MiB, 1% of `text`/`ascii` up to
  16 KiB) this drives some partitions past 64 KiB so the indexed
  point-read pass actually has bug-3-eligible targets to seek into.

Phase outputs measured per side:
- Wall-clock duration
- Bytes processed throughput (MiB/s)
- Partitions / rows processed per second
- Number of output SSTables (live, after multi-pass UCS settles)
- Cursor-vs-control speedup multiplier

---

## What it does NOT test (yet)

The validator focuses tightly on **compaction-output identity**. The
following are out of scope today:

- **Read-path semantics.** It doesn't issue CQL reads through
  `StorageProxy`. The depth-checks pass exercises reverse iteration and
  indexed point-reads at the SSTable layer, which catches the cursor
  bugs that produce divergent on-disk byte streams under those access
  patterns. Higher-level filter/paging/tombstone-purge behaviour driven
  by the read coordinator is still uncovered.
- **Repair, streaming, or hint replay.** Single-node, offline, no
  network.
- **Counter columns.** Cursor compaction's `unsupportedSchema` rejects
  them, so the schema generator excludes them.
- **Non-frozen collections.** Cursor rejects "complex" columns; the
  generator only emits `frozen<set<...>>`, `frozen<list<...>>`,
  `frozen<map<...>>`, and tuples (which are implicitly frozen).
- **Vector columns and secondary indexes.** Same reason — cursor doesn't
  support them yet.
- **Non-Murmur3 partitioners.** Cursor requires `supportsReusableKeys()`,
  which only Murmur3 provides today.
- **Per-side `disk_access_mode`.** `DatabaseDescriptor.disk_access_mode`
  is JVM-global; both sides must specify the same value (or both omit
  it). Comparing across modes requires running the validator twice.
- **Range-tombstone and partition-tombstone generation.** The
  `DataGenerator` emits cell tombstones (via `INSERT NULL`) but no
  `DELETE FROM t WHERE pk=? [AND ck >= ?]` mutations. With the GCGS
  rotation in place the corresponding compaction code paths are
  reachable but currently unexercised — a follow-on.
- **N-way comparison in one run.** The tool runs strict A vs B per run.
  Sweeping over a matrix of configurations means orchestrating multiple
  runs externally.
- **Schema-aware mismatch repro.** When a mismatch lands, the preserved
  input is the SSTable set as written by `CQLSSTableWriter`. Cell
  timestamps in those SSTables come from wall-clock time, so a fresh
  re-run with the same seed produces logically-identical content but
  different cell timestamps. The repro is "logical", not "byte-exact",
  for the SSTable bytes themselves — though both sides of a single run
  always see the same timestamps, so the validator's identity check is
  unaffected.

---

## Quick start

```bash
# 1. Build the tool jar (first build pulls Lanterna; later builds are fast)
ant compaction-validator-jar

# 2. Run with the shipped sample config (legacy iterator vs cursor)
tools/bin/compaction-validator \
    --config tools/compaction-validator/configs/sample.yaml \
    --once \
    --working-dir /tmp/cv \
    --log-file /tmp/cv.log
```

The TUI opens in your terminal, walks through Schema → Data Generation
→ Compaction → Validation → PASS/FAIL summary. With `--once` it exits
after one run; without it the loop continues with new seeds until you
press `q` or the optional `run.max_runs` cap is hit.

The default sample config generates 10 GiB of source data per run. To
get a faster first run, copy and edit:

```bash
cp tools/compaction-validator/configs/sample.yaml /tmp/quick.yaml
# Set run.target_bytes: 200M
tools/bin/compaction-validator --config /tmp/quick.yaml --once \
    --working-dir /tmp/cv --log-file /tmp/cv.log
```

---

## Building

The validator is an Ant subproject under `tools/compaction-validator/`.
The shipped targets:

| Ant target | What it does | Output |
|---|---|---|
| `compaction-validator-build` | Compile main sources | `build/classes/compaction-validator/` |
| `compaction-validator-build-test` | Compile tests | `build/test/compaction-validator-classes/` |
| `compaction-validator-jar` | Build the runnable jar | `build/tools/lib/compaction-validator.jar` |
| `compaction-validator-test` | Run JUnit tests | XML reports in `build/test/output/` |

Prerequisites:
- JDK 17 (works) or JDK 21+ (recommended — required for the default
  generational ZGC settings; see the launcher script's
  `ANTHROPIC_GC_ALGO` knob if you're on JDK 17).
- Ant 1.10+.
- The first build runs `ant resolver-retrieve-build` transitively, which
  pulls `lib/lanterna-3.1.2.jar` from Maven Central. Subsequent builds
  reuse the local copy.

The launcher script `tools/bin/compaction-validator` sources
`tools/bin/cassandra.in.sh` so the build classpath includes
`build/classes/compaction-validator/` automatically — no install step
beyond the jar build.

---

## Running

### Command-line flags

The CLI surface is intentionally slim. Most knobs live in the YAML
config; only environment-specific overrides are CLI flags.

| Flag | Required | Description |
|---|---|---|
| `--config / -c <path>` | **yes** | Path to the YAML config file. Without it the tool prints `Missing required option: '--config'` and the help text. |
| `--seed / -s <hex\|dec>` | no | Override the per-run RNG seed for reproducing a previous run. Accepts decimal (`12345`) or `0x`-prefixed hex (`0xCAFE...`). When omitted, the first run uses a random seed and subsequent runs derive new seeds via SplitMix64. |
| `--working-dir / -w <path>` | no | Working-directory base. Per-run subdirectories `<working-dir>/<seed-hex>/{source,output-legacy,output-cursor}/` are created here. Default: `${TMPDIR:-/tmp}/cassandra-compaction-validator`. |
| `--log-file / -l <path>` | no | Append-only structured run log. Default: `./compaction-validator.log`. |
| `--once` | no | Exit after one run (useful for CI and reproducing failures). Overrides `run.max_runs` in the config. |
| `--help / -h` | no | Show usage. |
| `--version / -V` | no | Print the version. |

### Environment variables

These are read by the launcher script (`tools/bin/compaction-validator`):

| Variable | Default | Purpose |
|---|---|---|
| `MAX_HEAP_SIZE` | `24G` | JVM `-Xmx`. The default is generous because cursor compaction's offline pipeline needs room for its scanners + writers + the validator's merge iterators. Drop to `4G–8G` for small targets. |
| `ANTHROPIC_GC_ALGO` | `-XX:+UseZGC -XX:+ZGenerational -XX:+ZUncommit` | GC algo + flags. **Requires JDK 21+.** On JDK 17, set to `-XX:+UseG1GC`. |
| `CV_GC_LOG` | `/tmp/compaction-validator-gc.log` | JVM unified-GC log path. |
| `JAVA`, `CLASSPATH`, `JVM_OPTS`, `JAVA_AGENT` | sourced from `cassandra.in.sh` | Standard Cassandra-tools wiring. |

The launcher also sets `cassandra.storagedir` to
`<working-dir>/_cassandra-system` and **wipes it on every launch**. This
is the system-keyspace + commitlog + saved-caches root used during
daemon initialization; wiping it prevents one run from inheriting a
corrupt transaction log from a prior crash.

---

## YAML configuration reference

Top-level structure:

```yaml
version: 1                          # required; forward-compat schema version

run: { ... }                        # run-loop knobs (see below)
schema: { ... }                     # cross-side schema overrides
comparison:
  control: { ... }                  # required; baseline / known-good
  experiment: { ... }               # required; variation under test
```

### `run:` block — run-loop knobs

| Key | Type | Default | Description |
|---|---|---|---|
| `target_bytes` | string | `10G` | Approximate source-data size per run. Strings: `100M`, `1.5G`, `500MiB`, `10GiB` — parsed by `ByteUtil.parseBytes`. |
| `datagen_threads` | int | `Runtime.availableProcessors()` | Parallel `CQLSSTableWriter` writers. Each thread runs to its own deterministic per-thread byte quota so the same `(seed, threads)` always produces the same data. |
| `compaction_threads` | int | `1` | Concurrent compaction tasks PER SIDE. Same value applied to both sides so the speedup measurement isn't perturbed by asymmetric parallelism. UCS returns batches of independent (different-shard) tasks, so values up to `base_shard_count` typically scale nearly linearly. |
| `validation_threads` | int | `1` | Token-range shards in Phase A. The Murmur3 ring is split into N equal-width ranges; each worker handles one. The canonical mismatch report (smallest-token result) stays deterministic regardless of N. |
| `max_runs` | int | `0` | Stop after this many runs. `0` = unlimited. |
| `no_cleanup` | bool | `false` | Preserve the per-run working directory even on PASS. Useful for inspecting compaction outputs. |
| `no_ui` | bool | `false` | Use a plain stdout reporter instead of the Lanterna TUI. Auto-fallback also kicks in when no TTY is attached. |
| `ignore_errata` | list of strings | `[]` | Known-bug rule names to suppress (see [Errata system](#errata-system)). Each entry must match a real `ErrataRule.cliName()` — typos fail at config-load time. |

### `schema:` block — cross-side schema overrides

| Key | Type | Default | Description |
|---|---|---|---|
| `partitioner` | string | `Murmur3` | Currently only Murmur3 is supported (cursor compaction requires `supportsReusableKeys()`). |

The schema generator's column counts, types, clustering directions,
static-or-not, etc. are seed-driven and not currently configurable.

### `comparison.{control,experiment}:` block

Both sides have the same structure. Anything omitted on a side
inherits a per-seed-randomised value applied identically to BOTH sides
— so omitting an attribute means "I'm not testing this; let the seed
decide."

| Key | Type | Description |
|---|---|---|
| `name` | string | Display label used in TUI panels, log entries, and the `[compact:<name>]` plain-text lines. The keyspace suffix derives from this name (`<base>_<sanitized-name>`). |
| `pipeline` | string | Compaction pipeline. `ITERATOR` (or aliases `LEGACY` / `ITER`) for the legacy iterator pipeline; `CURSOR` for cursor-based compaction. Omit to randomize. |
| `format` | string | SSTable output format for this side's compaction. `BIG` or `BTI` (case-insensitive). Omit to inherit the JVM-global default (BIG, set in `Main.bootstrapJvm`). The combination `pipeline: CURSOR + format: BTI` is rejected at config-load time — `CursorCompactor.unsupportedSchema` only accepts BIG output. When the two sides agree on a format (or both omit it), compaction runs in parallel; when they differ, the two sides run serially while the JVM-global format is toggled around each. Source data (written by `DataGenerator`) always uses the JVM-global default; only each side's compaction *output* is affected. |
| `compaction.class` | string | Compaction strategy class. Examples: `UnifiedCompactionStrategy`, `SizeTieredCompactionStrategy`, `LeveledCompactionStrategy`, `TimeWindowCompactionStrategy`. |
| `compaction.options` | map | Strategy-specific options that go into the `WITH compaction = { ... }` clause. |
| `compression.class` | string | Compression codec class. Examples: `LZ4Compressor`, `ZstdCompressor`, `SnappyCompressor`, `DeflateCompressor`, or `null` for uncompressed. |
| `compression.options` | map | Codec-specific options. The `chunk_length_in_kb` option may also be specified as a top-level sibling of `class` for ergonomics. |
| `io_mode` | string | Cassandra `disk_access_mode` value (`standard`, `mmap`, `mmap_index_only`, etc.). MUST match across the two sides; per-side I/O modes require subprocess isolation and aren't supported yet. |

### Validation behaviour

The parser fails fast (before any run starts) on:
- Missing `version`, `run`, `comparison.control`, or `comparison.experiment`.
- Unknown YAML keys under any block (with a helpful "Allowed: [...]"
  message listing valid keys).
- Non-positive thread counts or negative `max_runs`.
- `comparison.control.io_mode != comparison.experiment.io_mode`.
- Unknown errata rule names (typos in `ignore_errata`).
- Type mismatches (e.g. a string where an int is expected).

---

## Example configurations

### 1. Legacy iterator vs cursor (the original use case)

This is what `configs/sample.yaml` ships with. It validates that
cursor-based compaction produces identical output to the legacy
iterator pipeline.

```yaml
version: 1

run:
  target_bytes: 10G
  datagen_threads: 8
  compaction_threads: 4
  validation_threads: 12
  no_cleanup: false
  no_ui: false
  ignore_errata: []

schema:
  partitioner: Murmur3

comparison:
  control:
    name: legacy
    pipeline: ITERATOR
    format: BIG                # BIG | BTI; null/omitted → JVM-global default (BIG)
    compaction:
      class: UnifiedCompactionStrategy
      options:
        target_sstable_size: 64MiB
        base_shard_count: 4
    compression:
      class: LZ4Compressor
      chunk_length_in_kb: 16
    io_mode: standard

  experiment:
    name: cursor
    pipeline: CURSOR
    format: BIG                # CURSOR + BTI is rejected at config-load
    compaction:
      class: UnifiedCompactionStrategy
      options:
        target_sstable_size: 64MiB
        base_shard_count: 4
    compression:
      class: LZ4Compressor
      chunk_length_in_kb: 16
    io_mode: standard
```

```bash
tools/bin/compaction-validator \
    --config configs/sample.yaml \
    --working-dir /tmp/cv-iter-vs-cursor \
    --log-file /tmp/cv-iter-vs-cursor.log
```

### 2. UCS vs STCS

Validate that running through UCS produces equivalent merged output
to size-tiered compaction. Both sides use the iterator pipeline so
the comparison is purely about the compaction strategy.

```yaml
version: 1

run:
  target_bytes: 5G
  datagen_threads: 8
  compaction_threads: 2
  validation_threads: 8

comparison:
  control:
    name: ucs
    pipeline: ITERATOR
    compaction:
      class: UnifiedCompactionStrategy
      options:
        target_sstable_size: 128MiB
        base_shard_count: 2

  experiment:
    name: stcs
    pipeline: ITERATOR
    compaction:
      class: SizeTieredCompactionStrategy
      options:
        min_threshold: '4'
        max_threshold: '32'
        bucket_low: '0.5'
        bucket_high: '1.5'
```

Note: the strategy `options` map values are strings — they're emitted
verbatim into the CQL `WITH compaction = { 'min_threshold': '4', ... }`
clause. Numeric values without quotes also work in YAML; the parser
coerces.

### 3. LZ4 vs Zstd compression

```yaml
version: 1

run:
  target_bytes: 2G
  no_ui: true               # plain text + log only

comparison:
  control:
    name: lz4
    pipeline: CURSOR
    compaction:
      class: UnifiedCompactionStrategy
      options:
        target_sstable_size: 128MiB
    compression:
      class: LZ4Compressor
      chunk_length_in_kb: 16

  experiment:
    name: zstd-3
    pipeline: CURSOR
    compaction:
      class: UnifiedCompactionStrategy
      options:
        target_sstable_size: 128MiB
    compression:
      class: ZstdCompressor
      options:
        chunk_length_in_kb: 16
        compression_level: '3'
```

This compares output identity across compressors. The validator's hash
sweep walks the *uncompressed* partition stream, so the comparison
isn't sensitive to byte-level differences in the compressed form.

### 4. UCS shard-count comparison (cursor vs cursor)

```yaml
version: 1

run:
  target_bytes: 5G
  compaction_threads: 4

comparison:
  control:
    name: shard-4
    pipeline: CURSOR
    compaction:
      class: UnifiedCompactionStrategy
      options:
        target_sstable_size: 256MiB
        base_shard_count: 4

  experiment:
    name: shard-16
    pipeline: CURSOR
    compaction:
      class: UnifiedCompactionStrategy
      options:
        target_sstable_size: 256MiB
        base_shard_count: 16
```

Both sides run the cursor pipeline; only the UCS shard count differs.
Useful for verifying the shard-count knob doesn't perturb output
identity. Bump `compaction_threads` because higher shard counts give
UCS more independent tasks per round to parallelize.

### 5. Compression chunk size comparison

```yaml
version: 1

comparison:
  control:
    name: chunk-4kb
    pipeline: CURSOR
    compression:
      class: LZ4Compressor
      chunk_length_in_kb: 4

  experiment:
    name: chunk-256kb
    pipeline: CURSOR
    compression:
      class: LZ4Compressor
      chunk_length_in_kb: 256
```

Verifies LZ4 chunk size doesn't perturb partition content. (The validator
operates on uncompressed unfiltereds, so this should always pass — but
it's a useful sanity check after compression-related changes.)

### 6. Long soak with known-bug suppression

```yaml
version: 1

run:
  target_bytes: 50G
  datagen_threads: 16
  compaction_threads: 8
  validation_threads: 16
  max_runs: 0                       # unlimited
  no_cleanup: false                 # delete working dir on PASS
  ignore_errata:
    - equal-ts-tiebreaker           # CursorCompactor.mergeCells() inverted tie-breaker

comparison:
  control: { name: legacy, pipeline: ITERATOR }
  experiment: { name: cursor, pipeline: CURSOR }
```

This is the soak-test shape: run continuously, suppress the known
cursor tie-breaker bug (so the run keeps going as occurrences are
recorded), and trip loudly on any *new* divergence. The history panel
on screen shows each run's seed + duration + result so you can see at
a glance how stable the soak has been.

To inspect what got suppressed afterward, grep the log:

```bash
grep -A1 'Errata:' /path/to/compaction-validator.log
```

### 7. Quick smoke test (CI-friendly)

```yaml
version: 1

run:
  target_bytes: 100M
  datagen_threads: 4
  compaction_threads: 2
  validation_threads: 4
  no_ui: true

comparison:
  control:    { name: legacy, pipeline: ITERATOR }
  experiment: { name: cursor, pipeline: CURSOR }
```

```bash
tools/bin/compaction-validator \
    --config /tmp/smoke.yaml --once \
    --working-dir /tmp/cv-smoke --log-file /tmp/cv-smoke.log
```

100 MiB target finishes in well under a minute on a laptop. With
`--once` and `--no-ui` (config or CLI) the output is grep-able log
lines suitable for a CI step.

### 8. BIG vs BTI cross-format comparison

```yaml
version: 1

run:
  target_bytes: 1G
  datagen_threads: 4
  compaction_threads: 12
  validation_threads: 12
  max_runs: 0
  ignore_errata:
    - equal-ts-tiebreaker
    - tombstone-expiring-flags-both-set
    - tombstone-resurrected-by-expiring
    - prev-unfiltered-size-zero
    - index-block-width-off-by-one
    - wide-column-subset-dropped-last

schema:
  partitioner: Murmur3

comparison:
  control:
    name: big
    pipeline: ITERATOR
    format: BIG
  experiment:
    name: bti
    pipeline: ITERATOR
    format: BTI
```

```bash
tools/bin/compaction-validator \
    --config tools/compaction-validator/configs/big-vs-bti.yaml
```

Both sides must use `ITERATOR` because cursor compaction's
`unsupportedSchema` only accepts BIG output (`pipeline: CURSOR + format:
BTI` is rejected at config-load time with a clear error). Cross-format
runs serialise the two compactions internally — the JVM-global
`DatabaseDescriptor.getSelectedSSTableFormat()` is read by
`ColumnFamilyStore.newSSTableDescriptor` at write time, so the validator
toggles it between sides and runs them sequentially. Expect roughly 2×
the wall-clock duration of a same-format parallel run.

On disk afterward, the two sides produce visually distinguishable file
names:

```
output-legacy/<keyspace>/<table>-<id>/pa-N-big-Data.db
                                       pa-N-big-Index.db
                                       pa-N-big-Statistics.db
                                       ...
output-cursor/<keyspace>/<table>-<id>/ea-N-bti-Data.db
                                       ea-N-bti-Partitions.db
                                       ea-N-bti-Rows.db
                                       ea-N-bti-Statistics.db
                                       ...
```

The validator's identity check works at the logical (partition + row +
cell) layer regardless of file format — the byte-level encoding differs
between BIG and BTI but the merged `Unfiltered` stream a reader yields
must agree.

### 9. Reproducing a known failure

When a run fails, the log entry includes a re-run command:

```
[2026-05-25T01:30:35Z] Run #2 seed=0x693F6A0B09368F0C
  ...
  Valid:   FAIL  |  static row col[0]: column metadata differs: ...
  Preserved: /tmp/cv/0x693F6A0B09368F0C
  Re-run:  compaction-validator --seed 0x693F6A0B09368F0C --once --no-cleanup
```

Pin the seed and disable cleanup to keep the input + output sets:

```bash
tools/bin/compaction-validator \
    --config configs/sample.yaml \
    --seed 0x693F6A0B09368F0C \
    --once \
    --working-dir /tmp/cv \
    --log-file /tmp/cv-repro.log
```

Add `no_cleanup: true` to the YAML (or look at the preserved dir from
the original failure), then inspect the source + per-side outputs
with `tools/bin/sstabledump`:

```bash
# Find SSTables containing the offending partition
for f in /tmp/cv/0x693F6A0B09368F0C/source/pa-*-big-Data.db; do
    if tools/bin/sstabledump "$f" -k '<partition-key>' 2>/dev/null | grep -q "$key"; then
        echo "$f"
    fi
done

# Dump just that partition with raw timestamps
tools/bin/sstabledump pa-25-big-Data.db -k 'wUZ' -t
```

---

## Output and reporting

### TUI

Default layout when run with a TTY (1 Hz refresh, designed for an
80×40+ terminal):

```
─── Cassandra Compaction Validator | Run #N | seed=0x... | Phase: [VALIDATING] | Elapsed: 00:04:29 ───

─── Schema ──────────────────────────────────────────────────────────
 CREATE TABLE cvtest_xxx.t (
   pk0 ascii, pk1 uuid, ck0 bigint, ck1 uuid, s0 int STATIC, ...
 ) WITH compaction = {...} AND compression = {...};

─── Data Generation ─────────────────────────────────────────────────
 [████████████████████████████████░░░] 1.8 GiB/2.0 GiB    ETA: 00:00
 1.4M partitions | 11.5M rows | avg 165 B/row
 77.2 MiB/s | 57.5K partitions/s | 488.7K rows/s

─── Compaction ──────────────────────────────────────────────────────
┌──── LEGACY ──────────────────────┐  ┌──── CURSOR ──────────────────────┐
│ T8 256M [░ 410M] [░ 421M] ...    │  │ T8 256M [░ 410M] [░ 277M] ...    │
│ T7 128M [░ 137M] [░ 138M]        │  │ T7 128M [░ 138M] [░ 137M]        │
│ T6 64M  [░ 84M] [░ 96M] [░ 105M] │  │ T6 64M  [░ 96M] [░ 84M] [░ 104M] │
│ Wrote 16 sstables (1.6 GiB)      │  │ Wrote 16 sstables (1.6 GiB)      │
│ [████████████████████████████]   │  │ [████████████████████████████]   │
│ 2.1M p/s | 372 MiB/s | 184M scanned | 16 sst │  │ 3.4M p/s | ... │
└──────────────────────────────────┘  └──────────────────────────────────┘

─── Validation ──────────────────────────────────────────────────────
LEGACY  [████░░░░░░░░░░░░░░░░] 47% | 678K partitions | reading 16 sstables
CURSOR  [████░░░░░░░░░░░░░░░░] 47% | 678K partitions | reading 16 sstables

─── Recent Runs ─────────────────────────────────────────────────────
 #1  18:42:11→18:42:38   00:27   0xCAFEBABE...  341 MiB   PASS
 #2  18:42:45→18:43:12   00:27   0xDEADBEEF...  342 MiB   PASS

 q=quit                                            Heap: 18.28/24.00 GB (76%)
```

Press `q` (or Esc) to exit. On failure the SummaryPanel shows a
detailed inspection view with full schema, mismatch description, and
both sides' partition dumps; supports ↑/↓/PgUp/PgDn/Home/End to scroll
through long dumps.

### Plain-text mode (`run.no_ui: true` or no TTY)

```
[run 1] Starting run with seed=0xCAFEBABE12345678
[run] comparison: control=legacy  experiment=cursor
[schema] keyspace=cvtest_12345678 table=t pk=2 ck=1 static=2 regular=5
[datagen] 28.1 MiB/200.0 MiB (14.0%)  partitions=54651 rows=460615  109.8 MiB/s
[datagen] complete: 200.3 MiB in 4.3s (78.5 MiB/s)
[compact:legacy] task 1 of 2 started: 4 input sstables, 200.0 MiB
[compact:cursor] task 1 of 2 started: 4 input sstables, 200.0 MiB
[compact:cursor] complete: 3.2s (228.8 MiB/s, 1417300 partitions)
[compact:legacy] complete: 7.5s (97.8 MiB/s, 1417304 partitions)
[validate] partitions checked: 86009
[validate] complete: PASS (708807 partitions, 1410595 rows)
[run 1] PASSED in 20.6s (cursor speedup 2.34x)
```

Compaction-progress lines are throttled to ~1/sec per side. Mismatch
reports dump full schema + per-side partition contents under
`--- CONTROL OUTPUT ---` / `--- EXPERIMENT OUTPUT ---` banners.

### Run log

Append-only at the path passed via `--log-file`. Header on launch:

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

Per run:

```
[2026-05-24T18:49:09Z] Run #47 seed=0xDEADBEEF12345678
  Schema:  cvtest_deadbeef.t  (2 PK, 1 CK, 2 static, 5 regular cols, shape=NARROW, gc_grace=3600s)
  UCS:     target_sstable_size=256MiB  base_shard_count=2  compression_chunk=16KiB
  Schema CQL:
    CREATE TABLE cvtest_deadbeef.t (...) WITH compaction = {...} AND gc_grace_seconds = 3600;
  DataGen: 10.3 GiB  |  1.4M partitions  |  52M rows  |  310 MiB/s avg
  legacy:  18.4s  |  290 MiB/s  |  1.4M partitions/s
  cursor:  15.7s  |  340 MiB/s  |  1.6M partitions/s  |  Speedup: 1.17x
  Valid:   PASS  |  1.4M partitions matched  |  52M rows matched
  Errata:  3 suppressed (run still PASS):
    equal-ts-tiebreaker  3
  Format:  2 violation(s) on experiment side:
    flag-delete-and-expire  1
    scanner-failure         1
```

Each line:
- `Schema:` includes the schema's rolled `shape` (`NARROW` / `WIDE`) and
  the per-run `gc_grace_seconds`. Useful for `grep` filtering the soak
  log by structural / GC dimension.
- `Errata:` prints once per run when at least one rule matched. Cell
  divergences and format-violation suppressions both flow through here
  (the format-violation kinds map to errata rules via
  `ErrataRule.forFormatViolation(kind)`).
- `Format:` prints once per run when the `FormatAuditor` reported any
  cell-/row-level invariant violations on the experiment side, with
  per-kind counts and the first ~32 violation descriptions inline.
  Suppressed by an active errata = run still PASS; unsuppressed = run
  FAILS (the unsuppressed list is folded into the failure detail).

On failure: `Valid:   FAIL  |  <description>`, plus `Preserved:` and
`Re-run:` lines, plus a full `Mismatch detail:` block (the
`MismatchReport.formatForDisplay` output, indented).

### Where preserved data lives

On PASS, the per-run dir is removed (unless `run.no_cleanup: true`).
On FAIL it stays at `<working-dir>/<seed-hex>/` with three subdirs:

```
<working-dir>/<seed-hex>/
  source/                         (read-only after data-gen)
  output-legacy/<keyspace>/<table>-<tableId>/   (control-side compaction output)
  output-cursor/<keyspace>/<table>-<tableId>/   (experiment-side compaction output)
```

Inspect with `tools/bin/sstabledump` — see Example 8 above.

---

## How it works (high level)

```
[seed S] ──┬─► SchemaGenerator ─► CREATE TABLE CQL
           │     (rolls SchemaShape NARROW/WIDE,
           │      gc_grace_seconds, NULL fraction,
           │      compaction + compression specs)
           │
           ├─► DataGenerator ──► /source/*.db    (one set, hard-linked into both sides)
           │     (multi-row partitions w/ shared PK
           │      when CK ≥ 1; per-row TTL bind;
           │      power-law cell sizes; wide-row
           │      partitions; UNSET column omits
           │      for sparse-row encoding)
           │
           ├─► hard-link into /output-legacy/*.db ──► CompactionDriver(control)  ┐
           │                                          (pipeline + strategy + compression
           │                                            + format from comparison.control)
           │                                                                    │
           │                                                                    ├─► merged
           ├─► hard-link into /output-cursor/*.db ──► CompactionDriver(experiment)│   output
           │                                          (pipeline + strategy + compression
           │                                            + format from comparison.experiment)
           │                                                                    ┘
           │   (ParallelCompactor: parallel when both sides agree on format /
           │    serial+toggle when they differ; single pinned nowInSec
           │    shared across both drivers so tombstone-GC / TTL-purge
           │    decisions are deterministic and identical)
           │
           ├─► FormatAuditor ──► byte-/semantic-level invariants on
           │     experiment-side SSTables, parallelised across CPU pool
           │     (cell flag mutual exclusivity, row ordering, timestamp/TTL
           │     non-negativity, scanner-failure cascade)
           │
           └─► Validator
                 ├─ Phase A (parallel hash sweep) — N token-range workers walk
                 │              both sides in lockstep, hash & compare
                 ├─ Phase Depth-Checks (parallel, sampled 1-in-100) —
                 │              combined per-shard forward walk that does
                 │              per-partition reverse-iter hash + indexed
                 │              point-read on the partition's last clustering
                 └─ Phase B (cell-by-cell deep equals on first hash mismatch)
                            → MismatchReport, or empty Optional on PASS

  ▼ ErrataChecker on every Phase A divergence:
    cell-pair / value-only / row-asymmetry / column-extra matchers
    classify whether the divergence fits a known cursor-compaction bug.
    Format violations from the auditor go through the same suppression
    decision via ErrataRule.forFormatViolation(kind).
```

For the full data flow, JVM-bootstrap details, and design decisions
behind each phase, see [`SPEC.md`](../../SPEC.md) sections 5–17.

---

## Determinism guarantees

- **Same seed + same YAML config + same thread counts** → identical
  schema, partition keys, row values, partition-row counts, row-cell
  counts. Fully byte-identical at the logical-content level.
- **Cell timestamps differ across runs** because `CQLSSTableWriter` uses
  `System.currentTimeMillis()` for cell timestamps. Both sides of a
  given run see the same timestamps, so the validator's identity check
  is unaffected. But two separate runs of the same seed produce
  SSTable files that differ in the timestamp bytes.
- **Different `comparison_threads`, `validation_threads`** → no impact
  on schema or row data. Per-thread quotas in `DataGenerator` make
  per-thread output independent of the total thread count, so even
  changing `datagen_threads` only changes how the work is divided —
  not the resulting data set.
- **Failures are reproducible** via `--seed <hex>`. The "Re-run:" line
  in the log entry gives a copy-pasteable command.
- **Mismatch ordering is deterministic** under parallel validation:
  the canonical mismatch is the smallest-token result across all Phase A
  shards, so the same seed always produces the same `MismatchReport`
  regardless of `validation_threads`.

The `DataGeneratorDeterminismDriver` (a stand-alone main class — not a
JUnit test) checks all this:

```bash
CP="build/classes/compaction-validator:build/tools/lib/compaction-validator.jar:build/apache-cassandra-7.0-SNAPSHOT.jar"
for j in build/lib/jars/*.jar; do CP="$CP:$j"; done
java -Xmx256m -cp "$CP" \
    org.apache.cassandra.tools.compactionvalidator.datagen.DataGeneratorDeterminismDriver
```

It verifies the per-thread RNG sequence, that distinct thread indices
produce distinct row sequences (the bug that the SplitMix64 finalizer
in `SeedUtil.threadDataSeed` was added to fix), and that thread 0 of
N=8 produces the same data as thread 0 of N=1.

---

## Errata system

Real cursor-compaction bugs surface during long soak runs. Halting on
every occurrence stops productive testing while a fix is pending.
`--ignore-errata` (via `run.ignore_errata` in YAML) lets the validator
recognize specific bug *patterns*, count occurrences, and continue —
while still halting on any *new* divergence that doesn't match a
known rule.

### How it works

When Phase A flags a hash mismatch, the validator re-scans that single
partition and asks `ErrataChecker.matches(...)` whether *every*
observed difference fits the signature of an active errata rule. If
yes, the per-rule counter is bumped and Phase A continues. If any
difference is *not* covered, the partition surfaces as a real failure
and Phase B's full `MismatchReport` is built.

The matcher is deliberately strict: a rule fires only on *exactly* the
known signature. A new variant — same column, different field — is
treated as a real bug.

### Active rules

| CLI name | What it suppresses |
|---|---|
| `equal-ts-tiebreaker` | `CursorCompactor.mergeCells()`'s `COMPARE` branch inverts the tie-breaker direction at line 694. When two source SSTables provide live cells for the same `(partition, column)` at the same timestamp + TTL + `localDeletionTime`, cursor picks the cell with the **smaller** value bytes; legacy correctly picks the larger one per `Cells.resolveRegular()`. The matcher fires when timestamps + TTL + localDeletionTime + counter-flag all match but values differ. |
| `tombstone-expiring-flags-both-set` | Bug 1A: cursor compaction sets BOTH `IS_DELETED_MASK` and `IS_EXPIRING_MASK` in a cell's flags byte. Detected via the `FormatAuditor`'s expiration-formula invariant — the deserialiser's `else if` chain takes the IS_EXPIRING branch and reads `localDeletionTime` as a deletion-time instead of `writetime + ttl`, so the formula breaks. The errata matcher fires when the cursor cell shows this signature. |
| `tombstone-resurrected-by-expiring` | Bug 1B (silent data loss): when reconciling a tombstone vs an expiring cell at the same timestamp, `Cells.resolveRegular()` requires the tombstone to win. Cursor compaction reverses the choice and emits the expiring cell, resurrecting deleted data. The matcher fires on cell pairs where legacy is a tombstone, cursor is expiring, and timestamps match. |
| `prev-unfiltered-size-zero` | Bug 2: cursor compaction always writes `prevUnfilteredSize=0` on row/RTM headers. Forward iteration is unaffected; reverse iteration uses the field to seek backward and produces the wrong sequence. Caught by the validator's reverse-scan pass — the rule has no per-cell matcher (the field is purely metadata) and exists for soak-run suppression. |
| `index-block-width-off-by-one` | Bug 3: cursor compaction's `Index.db` final-block width is off by one for partitions ≥ 64 KiB. End-of-partition indexed point reads land in the wrong slice. Caught by the validator's indexed point-read pass. The rule has no per-cell matcher and exists for suppression. |
| `wide-column-subset-dropped-last` | Bug 4: cursor compaction's column-subset row encoder drops the highest-position cell from sparse rows in tables with ≥ 64 regular columns. The dropped cell shows up as legacy-has-cell-cursor-doesn't divergence in the cell-by-cell comparator. The errata rule reserves the name for future row-level matching; for now it's a suppression marker. |

Adding more rules is a matter of adding an enum entry — see [Adding a
new errata rule](#adding-a-new-errata-rule).

### Reporting

In the TUI/log, errata-suppressed runs still report PASS, but with a
prominent block listing each rule's hit count:

```
[validate] ╔══════════════════════════════════════════════════════════════════════════╗
[validate] ║ ERRATA SUPPRESSED — 3 partition(s) matched a known cursor-compaction bug ║
[validate] ╠══════════════════════════════════════════════════════════════════════════╣
[validate] ║  rule=equal-ts-tiebreaker        count=3
[validate] ║     <description>
[validate] ╚══════════════════════════════════════════════════════════════════════════╝
```

Run `grep -A2 'Errata:' /path/to/log` to count occurrences across a
soak.

---

## Resource expectations

For a 10 GiB target run:

| Resource | Approximate need |
|---|---|
| Disk | ~10 GiB source + 2× compaction outputs ≈ 30 GiB peak. With hard-linked source-into-output it's closer to 12–15 GiB peak. Set `--working-dir` somewhere with at least 40 GiB free. |
| RAM | The default 24 GiB `MAX_HEAP_SIZE` works for 10 GiB targets with `compaction_threads ≤ 4` and `validation_threads ≤ 16`. Bump for larger targets / more parallelism. Drop to 4–8 GiB for 100 MiB smoke runs. |
| Cores | `datagen_threads = num cores`, `compaction_threads = base_shard_count` (so UCS has one task per core), `validation_threads = up to ~num_cores` are reasonable starting points. |
| Wall clock | Highly hardware-dependent. On a 32-core box: ~30s data gen + 2–3 min compaction + a few seconds validation for a 10 GiB run with default config. |

The progress bars + ETA in the TUI give a more accurate per-run
estimate once a phase has been running for ~10 seconds.

---

## Troubleshooting

**`Missing required option: '--config=<configFile>'`**
The CLI requires `--config <path>`. There's no built-in default to keep
the user explicit about what's being tested.

**`Failed to load --config <path>: Unknown key 'X' under run`**
The parser rejects unknown YAML keys. The error message lists allowed
keys for the section. Check for typos like `target-bytes` (should be
`target_bytes`) or `data_gen_threads` (should be `datagen_threads`).

**`Failed to load --config: comparison.control.io_mode (X) must equal comparison.experiment.io_mode (Y)`**
Per-side I/O modes aren't supported in the same JVM (see the I/O mode
discussion in [What it does NOT test](#what-it-does-not-test-yet)).
Either match the values or omit them on both sides.

**Tool starts but TUI doesn't render / says "TUI unavailable"**
You're running without a controlling TTY (e.g. piped output, a CI
environment, or some Docker setups). The validator falls back to
plain-text output automatically. Force it with `run.no_ui: true` to
skip the TUI-init attempt.

**`AmazonCorrettoCryptoProvider has not passed the health check` warning**
You're on macOS but only the Linux ACCP jar is on the classpath, or
vice-versa. Drop the matching `AmazonCorrettoCryptoProvider-*-osx-aarch_64.jar`
(Apple Silicon) or `*-osx-x86_64.jar` (Intel Mac) into `lib/<arch>/`
and re-run. The launcher script filters by the `-linux-` / `-osx-`
filename infix so both jars can coexist there.

**`Unrecognized VM option 'ZGenerational'`**
You're on JDK 17 but the launcher defaults to generational ZGC, which
needs JDK 21+. Set `ANTHROPIC_GC_ALGO="-XX:+UseG1GC"` (or upgrade the
JDK).

**OOM / heap pressure**
Default heap is 24 GiB. If you have less RAM, set `MAX_HEAP_SIZE=4G`
(or whatever fits). Validator memory is roughly:
`compaction_threads × per-task scanner+writer buffers + validation_threads × per-shard scanner set`.
For small target_bytes, 4 GiB is plenty.

**`LEAK DETECTED` warnings on shutdown after `--no-cleanup`**
Expected. With `no_cleanup: true` the per-side CFS doesn't drop its
output `SSTableReader` refs (so the files stay on disk for inspection).
The next GC tidies the unreleased refs and Cassandra's leak watchdog
logs each one. Harmless if you're using `--once` (process exits
before the leaky GC runs).

**Validator output dir grew much larger than the source**
Check that you're running a recent build —
`LifecycleTransaction.waitForDeletions()` is called between UCS rounds
to drain obsoleted files. Without it, multi-pass UCS leaves obsolete
SSTables on disk and the dir balloons to N× input size after N rounds.

**Partition-count numbers in the TUI / log don't match expectations**
The compaction panel's `<X> scanned` is cumulative-across-passes — UCS
runs many passes and re-reads partitions each time, so for a 1.4M
unique-partition run the value can hit 100M+. Validation reports
unique partitions only. See SPEC.md §17 for the rationale.

---

## Project layout

```
tools/compaction-validator/
├── README.md                              this file
├── SPEC.md (at repo root)                 build-from-scratch reference
├── build.xml                              ant subproject
├── configs/
│   └── sample.yaml                        starting-point YAML
├── src/org/apache/cassandra/
│   ├── db/compaction/
│   │   ├── PipelineSelector.java          ITERATOR / CURSOR enum + factory
│   │   └── DirectCompactionRunner.java    bridge into CompactionTask
│   └── tools/compactionvalidator/
│       ├── Main.java                      picocli entry, JVM bootstrap
│       ├── RunLoop.java                   continuous-run loop
│       ├── RunOrchestrator.java           one-run pipeline
│       ├── RunResult.java                 immutable result record
│       ├── RunSeed.java                   SplitMix64 seed progression
│       ├── ProgressTap.java               reporter SPI
│       ├── PlainTextReporter.java         --no-ui sink
│       ├── compaction/
│       │   ├── CompactionDriver.java      drives one CFS through compaction
│       │   ├── CompactionStats.java
│       │   ├── ParallelCompactor.java     runs control + experiment concurrently
│       │   └── SstableSetManager.java     dir layout + hard-linking
│       ├── config/
│       │   ├── RunConfig.java             top-level YAML root
│       │   ├── RunSettings.java
│       │   ├── SchemaOverrides.java
│       │   ├── ComparisonConfig.java
│       │   ├── SideConfig.java
│       │   ├── CompactionSpec.java
│       │   ├── CompressionSpec.java
│       │   └── ConfigParser.java          SnakeYAML-backed loader
│       ├── data/
│       │   ├── DataGenerator.java         parallel CQLSSTableWriter driver
│       │   ├── DataGenStats.java
│       │   └── DataGeneratorDeterminismDriver.java   stand-alone main class
│       ├── logging/
│       │   ├── RunLogger.java             append-only run log
│       │   └── SystemInfo.java
│       ├── schema/
│       │   ├── SchemaGenerator.java       seed-driven schema generator
│       │   ├── GeneratedSchema.java       schema + per-side CQL builder
│       │   └── ColumnInfo.java
│       ├── tui/
│       │   ├── TuiManager.java            Lanterna lifecycle + render loop
│       │   ├── ProgressBus.java           event queue
│       │   ├── TuiEvent.java              sealed event hierarchy
│       │   ├── HeaderPanel.java
│       │   ├── SchemaPanel.java
│       │   ├── DataGenPanel.java
│       │   ├── CompactionPanel.java       side-by-side LSM widgets
│       │   ├── LsmTreeWidget.java         size-tier sstable display
│       │   ├── ValidationPanel.java
│       │   ├── SummaryPanel.java          post-run + scrollable failure inspector
│       │   ├── RunHistoryPanel.java       recent-runs ledger
│       │   └── ...                        helpers (TuiUtil, RateMeter)
│       ├── util/
│       │   ├── ByteUtil.java              parse 10G, format 1.2 GiB
│       │   ├── SeedUtil.java              per-component seed derivation
│       │   └── RateTracker.java
│       └── validation/
│           ├── Validator.java             Phase A / Phase B driver
│           ├── PartitionHasher.java       deterministic partition hash
│           ├── PartitionComparator.java   cell-by-cell deep equals
│           ├── MismatchReport.java
│           ├── ValidationStats.java
│           ├── ErrataRule.java            known-bug enum
│           └── ErrataChecker.java         rule-based partition matcher
└── test/unit/                             JUnit tests
    └── org/apache/cassandra/tools/compactionvalidator/...
```

The `db.compaction` files live in `org.apache.cassandra.db.compaction`
specifically so `DirectCompactionRunner` can access `CompactionTask`'s
`protected` fields. The split-package classloader works because both
JARs are on a flat classpath (no `module-info.java`).

---

## Adding a new errata rule

Two files to touch.

### 1. Add an enum entry in `ErrataRule.java`

```java
public enum ErrataRule
{
    EQUAL_TIMESTAMP_TIEBREAKER(...) { ... },

    // New rule:
    NEW_RULE_NAME("new-rule-cli-name",
        "One-paragraph description of the bug, including the file:line where the "
        + "defect lives and the symptom signature.")
    {
        @Override
        boolean explainsCellValueDifference(Cell<?> legacy, Cell<?> cursor)
        {
            // Return true if the cell pair's value-only difference matches
            // this rule's signature. False (or just don't override) if your
            // rule applies to a different kind of divergence.
            return ...;
        }
    };
    ...
}
```

For rules that fire on differences other than cell values (range
tombstone markers, partition-level deletion, etc.), extend
`ErrataChecker` to dispatch on the relevant kind of divergence.

### 2. Add a unit test in `ErrataCheckerTest.java`

Synthesize the partition pattern that should fire your rule, run it
through `ErrataChecker.matches(EnumSet.of(NEW_RULE_NAME), ...)`, and
assert the rule fires. Add a negative case: a similar-but-different
divergence should NOT fire (returns empty set → real mismatch).

The test's helper utilities (`makePartition`, `makeRowWithCell`,
`intCell`) build synthetic `UnfilteredRowIterator`s without needing a
running Cassandra.

### Then

Users opt in via `run.ignore_errata: [new-rule-cli-name]` in their YAML.
Unknown rule names fail at config-load time with the full known-rule
list in the error message — typos surface immediately.

---

## Running the tests

```bash
ant compaction-validator-build-test

# Run the full suite via JUnitCore (avoids Cassandra's heavy ant test
# runner; works on JDK 17 and 21):
CP="build/classes/compaction-validator:build/test/compaction-validator-classes"
CP="$CP:build/tools/lib/compaction-validator.jar"
CP="$CP:build/apache-cassandra-7.0-SNAPSHOT.jar"
for j in build/test/lib/jars/*.jar build/lib/jars/*.jar; do CP="$CP:$j"; done

java -Xmx512m \
    --add-opens java.base/java.io=ALL-UNNAMED \
    --add-opens java.base/java.nio=ALL-UNNAMED \
    --add-opens java.base/java.lang=ALL-UNNAMED \
    --add-opens java.base/java.lang.ref=ALL-UNNAMED \
    --add-opens java.base/sun.nio.ch=ALL-UNNAMED \
    --add-opens java.base/java.util.concurrent=ALL-UNNAMED \
    --add-exports java.base/jdk.internal.misc=ALL-UNNAMED \
    --add-exports java.base/jdk.internal.ref=ALL-UNNAMED \
    -cp "$CP" \
    org.junit.runner.JUnitCore \
    org.apache.cassandra.tools.compactionvalidator.validation.ValidatorTest \
    org.apache.cassandra.tools.compactionvalidator.validation.PartitionHasherTest \
    org.apache.cassandra.tools.compactionvalidator.validation.ErrataCheckerTest
```

Or via ant:

```bash
ant compaction-validator-test -Dtest.name=ErrataCheckerTest -Dno-build-test=true
```

The determinism driver (a stand-alone main class, not a JUnit test) is
in [Determinism guarantees](#determinism-guarantees) above.

---

## Future work

- **Per-side `disk_access_mode`** via subprocess isolation — one JVM per
  side, sharing the input set via on-disk hard links. Lets the
  validator compare standard ChannelProxy reads vs mmap.
- **Counter / vector / secondary-index columns** — gated on cursor
  compaction adding support for these.
- **Range-tombstone and partition-tombstone generation** in the data
  generator. Today only cell tombstones (via `INSERT NULL`) are emitted;
  with the GCGS rotation in place the corresponding compaction code paths
  are reachable but currently unexercised.
- **Direct byte-level checks for bugs 2 and 3** — the depth-checks pass
  catches them indirectly via reverse-iter divergence and indexed
  point-read divergence. A direct field check on the experiment-side
  `Data.db` (`prevUnfilteredSize > 0`) and `Index.db` (final-block width
  matches the `Data.db`-derived extent) would fire even when the 1-in-100
  sampling missed the affected partition.
- **Per-cell matcher for `WIDE_COLUMN_SUBSET_DROPPED_LAST`** — bug 4 is
  caught by the Phase B row diff today but the rule itself has no
  narrow per-cell predicate. Adding one would let the matcher count
  occurrences distinctly from generic `tombstone-resurrected-by-expiring`.
- **N-way comparison in one run** — currently the framework is strictly
  binary. A "matrix" config that schedules N×(N-1)/2 binary comparisons
  sequentially from one config would be useful for soak rigs.
- **Side-name labels in `RunHistoryPanel`** — the history panel shows
  the seed but not the configuration tag. With matrix-style soak runs,
  history rows ought to show a short config descriptor.
- **Schema-randomization knobs in YAML** — the column count / type
  weights are hardcoded. Exposing them would let the user bias soak
  runs toward specific schema shapes (today only `WIDE` probability is
  exposed implicitly via the seed).
- **Direction-aware errata rules** — current rules fire on the
  bug-pattern shape regardless of which side has the defect. Tagging
  rules with "buggy_side: experiment" and verifying the observed
  direction matches would catch the case where a rule fires for the
  wrong reason.
- **JFR / async-profiler integration in the launcher** — for
  performance investigation, having a `--profile` flag that wires up
  JFR with a sane default config would save a lot of fiddling.
- **Cell-timestamp determinism** — `CQLSSTableWriter` uses wall-clock
  microseconds. Plumbing a deterministic timestamp source through the
  writer would make the on-disk SSTable bytes themselves reproducible
  (today only the logical content is reproducible; the bytes encode
  timestamps that vary across runs, which is also why same-seed runs
  can hit subtly different cross-thread cell collisions).

---

## License

Apache License 2.0 (same as the rest of the Apache Cassandra project).
See the headers on individual source files.
