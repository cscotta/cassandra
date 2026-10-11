# Bug 1048: Thrift CAS Expected-Value Mismatch for Defined-Metadata Columns

**Commit**: 5034f44957 · **Ticket**: CASSANDRA-10576
**Subsystem**: Thrift / CAS (Compare-And-Swap)
**Category**: Incorrect filtering / wrong query result

---

## Root Cause
When a Thrift CAS operation carried an `expected` list and the table had columns with defined metadata (i.e., named/typed columns rather than raw dynamic columns), the expected partition was built eagerly from `LegacyLayout.toRowIterator` before being passed to `ThriftCASRequest`. The `appliesTo` check then compared that pre-built partition against the live partition read during the CAS protocol. Because the expected side was never run through `ThriftResultsMerger`, static columns were not translated into clusterings the same way the fetched partition was, causing the equality check to fail spuriously (CAS would refuse to apply even when the current state matched the expected state).

The fix defers conversion: `ThriftCASRequest` now stores the raw `LegacyCell` list and re-creates a `FilteredPartition` inside both `readCommand()` and `appliesTo()`, wrapping the expected iterator through `ThriftResultsMerger.maybeWrap` in `appliesTo()` so both sides are normalised identically before comparison.

### Before / After
```java
// BEFORE – expected built outside ThriftCASRequest; no ThriftResultsMerger applied
FilteredPartition partitionExpected = null;
if (!expected.isEmpty())
    partitionExpected = FilteredPartition.create(
        LegacyLayout.toRowIterator(metadata, dk, toLegacyCells(...).iterator(), nowInSec));
new ThriftCASRequest(partitionExpected, partitionUpdates)

// appliesTo() – raw partition compared without ThriftResultsMerger
for (Row e : expected) { ... }

// AFTER – raw cells stored; expected rebuilt inside appliesTo() with ThriftResultsMerger
new ThriftCASRequest(toLegacyCells(metadata, expected, nowInSec), partitionUpdates, nowInSec)

// appliesTo() – expected wrapped through ThriftResultsMerger before comparison
FilteredPartition expectedPartition = FilteredPartition.create(
    UnfilteredRowIterators.filter(
        ThriftResultsMerger.maybeWrap(expectedToUnfilteredRowIterator(), nowInSec), nowInSec));
for (Row e : expectedPartition) { ... }
```

---

## How to Spot
In CAS paths (any method that builds a `CASRequest`) look for cases where the expected and fetched partitions are produced by different code paths. If the fetched partition goes through a result-merging or format-normalisation layer that the expected partition does not, the `appliesTo` comparison will be asymmetric. Specifically, whenever Thrift's static-column-to-clustering translation (`ThriftResultsMerger`) is involved, both sides of the comparison must pass through it, or the row structures will differ structurally even when they represent the same logical data.

---

## Test
No dedicated test added for this commit (test files were not touched). The fix is verified by the existing Thrift CAS integration paths.
