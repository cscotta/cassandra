# Bug 1262: LCS Overlap Possible When All Candidate SSTables Contain the Same Single Partition

**Commit**: 917068dcbf · **Ticket**: CASSANDRA-9322
**Subsystem**: src/java/org/apache/cassandra/db/compaction/LeveledManifest.java
**Category**: Missing null / bounds check

---

## Root Cause

In `LeveledManifest`, when selecting higher-level sstables to include in a compaction, the code computes the key range `[min, max]` of the L0 candidate set and then uses it to build a `Range<RowPosition>` to find overlapping sstables in the higher level. When every candidate sstable covers exactly one partition (`min.equals(max)`), the resulting range is a degenerate zero-width range. A `Range` where both endpoints are identical is not a valid open interval in Cassandra's range model; the overlap check against higher-level sstables would either misfire or produce unexpected results, potentially causing the same single-partition sstables to be picked up by two concurrent compactions and leading to data overlap. The fix short-circuits out early when the range is degenerate.

### Before / After

```java
// BEFORE — no guard for single-partition (min == max) case
if (max == null || candidate.last.compareTo(max) > 0)
    max = candidate.last;
}
Set<SSTableReader> compacting = cfs.getDataTracker().getCompacting();
Range<RowPosition> boundaries = new Range<>(min, max);
```

```java
// AFTER — bail out before constructing an invalid range
if (max == null || candidate.last.compareTo(max) > 0)
    max = candidate.last;
}
if (min == null || max == null || min.equals(max)) // single partition sstables - we cannot include a high level sstable.
    return candidates;
Set<SSTableReader> compacting = cfs.getDataTracker().getCompacting();
Range<RowPosition> boundaries = new Range<>(min, max);
```

---

## How to Spot

Whenever code constructs a range from a dynamically computed `min` and `max`, validate that the resulting range is non-degenerate before using it for interval overlap or intersection queries. Ranges where `min == max` or where either bound is null may be legal in some data-structure APIs but semantically wrong in the business logic; a guard clause before range construction prevents silent misclassification of sstable sets.

---

## Test

No test added.
