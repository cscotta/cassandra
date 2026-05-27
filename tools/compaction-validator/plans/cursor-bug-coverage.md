# Plan — extend the validator to catch known cursor-compaction bugs

This plan covers four cursor-compaction defects that 300+ iterations of the
existing iterator-vs-cursor harness did not surface. See the analysis report
this plan accompanies for per-bug coverage gaps. Three extension axes are
proposed; together they close every gap.

| Phase | Axis | Bugs caught | Effort |
|---|---|---|---|
| 1 | Data-generation breadth | 1B, 4, contributes to 3 | small |
| 2 | Schema generator: wide tables + sparse rows | 4 | small |
| 3 | Format-level invariant audit | 1A, 2, 3 | medium |
| 4 | Read-path breadth (reverse scans + indexed point reads) | belt-and-suspenders for 2, 3 | medium |
| 5 | New `ErrataRule`s for each bug | all | small |
| 6 | Test harness coverage | regression-proof phases 1-5 | small |

Phase 1 + 2 + 5 (the cheap ones) get us past the data-generation gap and
unblock bug 1B + 4 immediately. Phase 3 catches the byte-level bugs (1A, 2, 3)
without needing new read-path code. Phase 4 is full belt-and-suspenders for
read-path-only bugs and is recommended but not strictly required to surface
the four bugs at hand.

---

## Phase 1: Data-generation breadth

Closes the "TTLs are never generated" gap (bug 1B) and lays the groundwork for
bug 3 (large cells / large partitions).

### 1.1 TTL generation

**File:** `tools/compaction-validator/src/.../data/DataGenerator.java`

Currently `writePartitions` calls `writer.addRow(values)`. `CQLSSTableWriter`
also offers `addRow(Map<String, Object>)` and `rawAddRow(...)` variants, but
the cleanest path for TTLs is to emit `INSERT ... USING TTL ?` in the prepared
statement with a TTL bind variable, set per-row.

Approach:
- Build a SECONDARY insert statement with `USING TTL ?` for the rows we want to
  expire. The TTL value is the last bind variable.
- Per row, decide via the rng:
  - 90% of rows: use the standard insert (no TTL) — same as today.
  - 5% of rows: use the TTL-bearing insert with a random TTL ∈ [60, 86400] seconds.
  - 5% of rows: use the TTL-bearing insert with a TTL ∈ [1, 30] seconds. These
    rows will likely expire mid-run, exercising the tombstone-vs-expiring
    reconciliation path that bug 1B relies on.

The probabilities and TTL ranges are intentionally varied so the bug's required
shape (same partition+column at same timestamp, one tombstone, one expiring)
arises naturally as soon as multiple writer threads' partition keys overlap.

Schema spec for the optional TTL bind variable: `INSERT INTO t (...) VALUES (?, ?, ...) USING TTL ?`.

### 1.2 Variable NULL fraction

Currently `NULL_FRACTION = 0.10` is a constant. Make it per-seed-randomized:
- Roll once per run from `[0.05, 0.50]` for the "normal" rotation.
- For wide-table runs (Phase 2 below), roll from `[0.70, 0.95]` to force the
  sparse-row encoding path bug 4 needs.

The rolled value is passed into `runWriterThread` and read in `buildRowValues`.

### 1.3 Larger cell values

Spec said "max 1MB" for variable-length columns; the implementation caps at
256 bytes for blobs and 64 chars for strings. Bump:
- `blob`: random length from 1 byte to 1 MiB (was: 1 to 256). Bias toward
  small (a power-law distribution would be ideal; an "80% small + 20% large"
  switch is fine for now).
- `text` / `ascii`: random length from 1 to 16384 chars.
- This lifts the largest partition past 64 KiB (the index-block threshold),
  enabling bug 3.

To avoid blowing up generation time, use a power-law-ish distribution
(99% under 4 KiB, 1% above). The `randomBytes(rng, minLen, maxLen)` helper
becomes a `randomBytesPowerLaw(rng, smallMax, largeMax, largeProbability)`.

### 1.4 Wide-row partitions (occasional)

Rows-per-partition currently rolls from `1 + rng.nextInt(16)` (so 1-16). Add
an occasional "wide-row" mode (5% of partitions): roll from `[100, 1000]`.
Combined with 1.3, these partitions consistently exceed 64 KiB and hit the
index-block boundary multiple times — the precondition for bug 3.

### Test plan

- `DataGeneratorDeterminismTest` re-runs to verify per-thread quotas still
  produce deterministic output with the new variation.
- New unit test `DataGeneratorTtlGenerationTest` exercises a fixed-seed run,
  reads back the SSTables, and asserts:
  - At least one cell has a non-zero `ttl()`.
  - At least one cell has `localDeletionTime != NO_DELETION_TIME` and `ttl == 0`
    (tombstone cell from NULL).
  - The TTL distribution matches the configured probabilities within 10%.
- Regression check: `ant compaction-validator-test` to confirm `ValidatorTest`,
  `ErrataCheckerTest`, `PartitionHasherTest`, `SstableSetManagerTest` still pass.

---

## Phase 2: Wide-table schemas + sparse rows

Closes bug 4.

### 2.1 Wide-table schema mode

**File:** `tools/compaction-validator/src/.../schema/SchemaGenerator.java`

`generate(rootSeed)` rolls a "shape" tag at the top of the function:
- 90% probability: the existing "narrow" shape (3-8 regular columns).
- 10% probability: "wide" shape with 64-200 regular columns + 0-32 static columns.

The wide shape exercises the column-subset encoding path that bug 4 lives in.
Tag the resulting `GeneratedSchema` with the shape so the run log + TUI can
report it (helps when triaging soak runs).

Implementation:
- Add a `SchemaShape { NARROW, WIDE }` enum.
- Add a `shape` field on `GeneratedSchema`.
- The narrow shape leaves all current behaviour unchanged.
- The wide shape allocates `regularCount` from `[64, 200]` and `staticCount`
  from `[0, 32]`. Same column-type distribution as today.

### 2.2 Wide-table NULL fraction coupling

Phase 1.2 introduces a per-run NULL fraction. For `WIDE` schemas, force the
fraction high enough that most rows are sparse (most columns absent), since
that's what the bug-triggering encoding path requires. Rolled value:
- `NARROW`: `[0.05, 0.50]`
- `WIDE`: `[0.70, 0.95]`

The wide-row clause in 1.4 is independent of schema shape — applying both at
once produces the worst-case (lots of rows × lots of columns × mostly null)
which is fine and tests bug 4 hardest.

### 2.3 Verify cursor compatibility

Cursor compaction's `unsupportedSchema` rejects non-frozen / "complex"
columns and counter columns; both already excluded by the schema generator's
type catalogue. No change needed.

`CursorCompactor.unsupportedSchema` does NOT reject high column counts. Verify
empirically by running `tools/bin/compaction-validator --once
--config <wide-config> --seed 0xCAFE...` against a forced-wide schema and
confirm both sides compact successfully.

### Test plan

- New unit test `WideSchemaTest`:
  - Generate 100 schemas with seeds biased to roll WIDE.
  - For each, assert `regularCount + staticCount >= 64`.
  - Assert the produced CQL parses.
- `SchemaGeneratorTest` (if it exists, otherwise add it) extended with
  shape-distribution and column-count-distribution checks.
- End-to-end: run `iterator-vs-cursor.yaml` with a fixed seed that rolls
  WIDE; confirm validation succeeds (or fails with a useful report) for at
  least one wide schema.

---

## Phase 3: Format-level invariant audit

Closes bugs 1A, 2, 3 by inspecting the cursor side's SSTable bytes directly
rather than relying on the tolerant standard reader.

### 3.1 New module: `validation/FormatAuditor.java`

A new class that, given an SSTableReader, walks its `Data.db` and `Index.db`
files and asserts a list of byte-level invariants. Returns a structured list
of violations (or empty for "clean").

Each violation is a `FormatViolation { side, sstable, partitionKey, kind,
description }` record so we can include it in the `MismatchReport` shape.

### 3.2 Invariants to check

1. **`IS_DELETED_MASK ↔ IS_EXPIRING_MASK` mutual exclusivity** (bug 1A).
   For every cell: `assert !((flags & IS_DELETED_MASK) != 0 &&
   (flags & IS_EXPIRING_MASK) != 0)`.

2. **`prevUnfilteredSize` non-zero where expected** (bug 2). For every
   row/RTM beyond the first in a partition: `assert prevUnfilteredSize > 0`.

3. **`Index.db` final-block width matches `Data.db` partition extent** (bug 3).
   For the last index entry of each partitioned-with-index partition:
   compute the actual width of the last index block from `Data.db` offsets
   and compare against the `Index.db`-stored width. They should be equal.

The auditor operates on raw byte buffers — uses
`org.apache.cassandra.io.sstable.format.big.BigFormat` types
(`UnfilteredSerializer.serializedSizeUnfiltered`, `RowIndexEntry`,
`SSTableReader.getPositionsForBounds`) where possible, falls back to direct
varint decoding when the public API doesn't expose what we need.

### 3.3 Wiring

After both compactions complete and before validation begins, the orchestrator
runs `FormatAuditor.audit` on the cursor side's SSTables. If violations exist,
they're attached to the `RunResult` as a separate `formatViolations` list.

The validator treats format violations as a **failure** by default. Each
class of violation gets its own `ErrataRule` (`ErrataRule.FORMAT_*`) so users
can opt to suppress known classes during soak runs.

The auditor doesn't run on the legacy side — the legacy iterator pipeline is
the format reference, and any divergence-relative-to-itself would be a
separate (more serious) bug.

### 3.4 Performance

The auditor is O(bytes) over the cursor side's output. For a 10 GiB run with
a fast disk this is on the order of a few seconds. To avoid doubling validation
time on huge runs, gate it on a config flag:
- `run.audit_format: true|false` (default `true` until we're confident the
  audit is correct, then `true` permanently).

### Test plan

- `FormatAuditorTest` (new, JUnit) constructs synthetic SSTables that
  deliberately violate each invariant (using direct byte writes) and asserts
  the auditor flags them.
- `FormatAuditorTest` also runs against bytes produced by the real legacy
  pipeline as a sanity check (these should always be clean).
- End-to-end: re-run `iterator-vs-cursor.yaml` against a fresh build —
  assuming the cursor bugs aren't fixed yet, the auditor should report
  bug 1A on every run, bug 2 on every run, bug 3 on runs that produce
  partitions ≥ 64 KiB.

---

## Phase 4: Read-path breadth — reverse scans + indexed point reads

Belt-and-suspenders for bugs 2 and 3, and catches future similar regressions.
Optional if Phase 3 is in place; recommended for completeness.

### 4.1 Reverse-scan pass

Add a Phase A.5 inside `Validator` that walks both sides backward through each
partition:
- Open scanners with `reversed=true` (via `SSTableReader.getScanner` /
  `iterator()` with reversed slices).
- Walk both sides backward, hashing each partition's reversed unfiltered
  stream.
- Compare hashes.

A reverse-scan divergence under cursor's broken `prevUnfilteredSize` would
manifest as either an exception (the iterator can't navigate) or a content
divergence (it returns the wrong rows).

### 4.2 Indexed point-read pass

For partitions ≥ 64 KiB on each side:
- Pick clustering keys at the START, MIDDLE, and END of the partition.
- Issue point reads via `SSTableReader.iterator(partitionKey, slices, false)`
  for each side.
- Compare the returned row(s).

The off-by-one in bug 3 manifests on the END-of-partition point read; the
seek lands one byte off and the cursor side returns the wrong row (or none).

### 4.3 Wiring

Both passes run after Phase A's hash sweep, only if Phase A passed (otherwise
the canonical mismatch from forward scan takes precedence). They share the
same shard infrastructure; reverse-scan uses the same token-range split.

### Test plan

- Synthetic `ReverseScanValidatorTest` — construct a partition with N rows,
  walk it reverse on both sides, assert identical sequences.
- Synthetic `IndexedPointReadValidatorTest` — construct a partition large
  enough to span ≥ 2 index blocks, do an end-of-partition point read on
  both sides, assert identical results.

---

## Phase 5: Errata rules for the four known bugs

Each bug becomes a named `ErrataRule` so soak runs can suppress known
patterns while still loudly halting on new ones.

### 5.1 New rules

**File:** `tools/compaction-validator/src/.../validation/ErrataRule.java`

```
TOMBSTONE_EXPIRING_FLAGS_BOTH_SET — bug 1A; matches a FormatViolation of
  kind FLAG_INVARIANT.

TOMBSTONE_RESURRECTED_BY_EXPIRING — bug 1B; matches when a partition pair
  differs only in cells where:
   - legacy has a tombstone (timestamp T, isExpiring=false, isDeleted=true)
   - cursor has an expiring cell (same T, isExpiring=true, isDeleted=false)
   - all other partition content matches

PREV_UNFILTERED_SIZE_ZERO — bug 2; matches a FormatViolation of kind
  PREV_SIZE_ZERO.

INDEX_BLOCK_WIDTH_OFF_BY_ONE — bug 3; matches a FormatViolation of kind
  INDEX_BLOCK_WIDTH.

WIDE_COLUMN_SUBSET_DROPPED_LAST — bug 4; matches when a partition pair
  differs only in the last present column of a sparse-row encoded row,
  legacy has the cell, cursor doesn't, and the table has ≥ 64 columns.
```

### 5.2 ErrataChecker extensions

`ErrataChecker.matches(...)` already takes a partition pair and an active
rule set. Extend it:
- For format-violation rules (1A, 2, 3), the matcher's input becomes the
  combination of partition pair + format-violation list. The orchestrator
  attaches relevant violations to the validation context.
- For 1B and 4, the existing pattern (cell-by-cell field comparison) extends
  naturally with new predicates on `ErrataRule`.

### 5.3 Documentation

Update `tools/compaction-validator/README.md` "Errata system" section with
the new rules, and `tools/compaction-validator/configs/iterator-vs-cursor.yaml`
gets all five rules in `run.ignore_errata` until they're fixed upstream.

---

## Phase 6: Test harness coverage

Lock the new behaviour with regression tests so coverage doesn't quietly
backslide.

- `DataGeneratorTest` (new) — assert TTL ratio, blob size distribution,
  rows-per-partition distribution match the configured shape.
- `WideSchemaTest` (new) — assert wide-shape probability, column count, and
  CQL roundtrip.
- `FormatAuditorTest` (new) — synthetic SSTables that violate / honor each
  invariant.
- `ErrataCheckerTest` (extended) — one positive + one negative case per new
  rule.
- An end-to-end smoke test that runs a fixed seed against a known-buggy
  cursor build and asserts:
  - At least one of the new rules fires.
  - The run still passes (errata-suppressed).

---

## Sequencing & estimated effort

| Phase | Files touched | LoC delta | Risk |
|---|---|---|---|
| 1 (data gen breadth) | 1 | ~150 | Low — adds optional behaviour, default-off paths gated by RNG |
| 2 (wide schemas) | 2 | ~80 | Low — new code path, narrow path unchanged |
| 3 (format auditor) | 3 (new auditor + Validator wire-up + RunResult field) | ~400 | Medium — touches BigFormat internals |
| 4 (reverse + point reads) | 1-2 | ~200 | Medium — new validator phase |
| 5 (errata rules) | 2 | ~100 | Low |
| 6 (tests) | 5 | ~300 | Low |

Recommended order:
1. Phase 1 + 2 + 5 (TTLs, wide schemas, errata rules for 1B + 4) — surfaces 1B + 4 immediately.
2. Phase 3 — surfaces 1A, 2, 3.
3. Phase 6 — locks in the regression coverage.
4. Phase 4 — additional read-path coverage; do once 1-3 are stable.

---

## Acceptance criteria

After all phases land:
- A fresh run of `iterator-vs-cursor.yaml` against an unfixed cursor build
  consistently surfaces (records-or-fails) all four bugs:
  - Bug 1A → `FormatAuditor` reports `FLAG_INVARIANT` violations on every run
    that contains a tombstone cell.
  - Bug 1B → `ErrataChecker` matches `TOMBSTONE_RESURRECTED_BY_EXPIRING`
    occasionally on runs that contain TTL-bearing cells in conflict with
    tombstones at the same timestamp.
  - Bug 2 → `FormatAuditor` reports `PREV_SIZE_ZERO` on every run.
  - Bug 3 → `FormatAuditor` reports `INDEX_BLOCK_WIDTH` on every run that
    produces a partition ≥ 64 KiB. Indexed point-read pass independently
    flags the same.
  - Bug 4 → `ErrataChecker` matches `WIDE_COLUMN_SUBSET_DROPPED_LAST` on
    runs that roll the WIDE schema shape with sparse rows.
- All existing tests still pass.
- `iterator-vs-cursor.yaml`'s `run.ignore_errata` lists the five new rules
  so soak runs continue to be useful while waiting on upstream fixes.
- README + SPEC are updated to document the new validation passes.
