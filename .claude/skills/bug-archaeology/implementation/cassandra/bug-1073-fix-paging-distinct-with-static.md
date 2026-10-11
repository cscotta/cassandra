# Bug 1073: Fix Paging of DISTINCT with Static and IN

**Commit**: 91e2501461 · **Ticket**: CASSANDRA-10354
**Subsystem**: src/java/org/apache/cassandra/db/filter/DataLimits.java, src/java/org/apache/cassandra/service/pager/MultiPartitionPager.java
**Category**: Incorrect filtering / wrong query result

---

## Root Cause

Two related bugs combined to corrupt paging of `DISTINCT` queries over partitions that contain only a static row when using an `IN` clause on the partition key. First, `DataLimits` incremented `rowCounted` when a lone static row was found but failed to increment `rowInCurrentPartition`, so the counter could fire the same static-row logic again on the next call, double-counting partitions. Second, `MultiPartitionPager` closed the current sub-pager's result iterator *after* calling `isExhausted()`, which consumed data from the next pager before returning the close, potentially skipping rows at page boundaries.

### Before / After

```java
// BEFORE (DataLimits.java)
if (hasLiveStaticRow && rowInCurrentPartition == 0)
    ++rowCounted;
```

```java
// AFTER (DataLimits.java)
if (hasLiveStaticRow && rowInCurrentPartition == 0)
{
    ++rowCounted;
    ++rowInCurrentPartition;
}
```

```java
// BEFORE (MultiPartitionPager.java)
if (isExhausted())
    return endOfData();
if (result != null)
    result.close();
```

```java
// AFTER (MultiPartitionPager.java)
if (result != null)
    result.close();
if (isExhausted())
    return endOfData();
```

---

## How to Spot

Counters that have multiple conceptual dimensions (rows-counted vs. rows-in-partition) are prone to partial updates: whenever one counter is incremented, check whether any sibling counter also needs updating to keep invariants consistent. Separately, resource-close ordering relative to boundary checks deserves scrutiny — closing a resource before checking exhaustion can advance state that the exhaustion check depends on.

---

## Test

No test added.
