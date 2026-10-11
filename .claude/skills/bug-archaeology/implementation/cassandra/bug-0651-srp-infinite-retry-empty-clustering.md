# Bug 0651: Short Read Protection Issues Infinite Retry for Skinny Tables

**Commit**: 35e32f20ba · **Ticket**: CASSANDRA-13880
**Subsystem**: src/java/org/apache/cassandra/service/DataResolver.java
**Category**: Logic error in condition

---

## Root Cause

For tables with no clustering columns each partition has at most one row whose clustering is `Clustering.EMPTY`. The short-read-protection `moreContents()` logic checks whether the row count changed since the last call to decide whether to fetch more rows. Because `Clustering.EMPTY` is the only possible clustering value, once that single row has been returned there is no more data to fetch — yet the old code never checked for this case and would issue repeated retry requests to the same replica. The fix adds an explicit early-return guard: if `lastClustering == Clustering.EMPTY` the method returns `null` immediately.

### Before / After

```java
// BEFORE – no guard for EMPTY clustering; retries indefinitely
if (lastCount == counter.counted() || !counter.isDoneForPartition())
    return null;
lastCount = counter.counted();
assert !postReconciliationCounter.isDoneForPartition();
// ... issue retry
```

```java
// AFTER – bail out early for tables with no clustering columns
if (lastClustering == Clustering.EMPTY)
    return null;
lastCount = counter.counted();
// ... issue retry
```

---

## How to Spot

Logic that iterates "until there is no more data" must explicitly handle degenerate table shapes — in particular, tables with no clustering columns (skinny tables) where `Clustering.EMPTY` is a sentinel meaning "this is the only possible row". Any loop or recursive-fetch routine that does not test for `Clustering.EMPTY` before requesting additional rows is a candidate for this class of bug.

---

## Test

No test added.
