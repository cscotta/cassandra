# Bug 0476: Index Queries on Partition Key Columns Miss Static-Only Partitions

**Commit**: 86e1590042 · **Ticket**: CASSANDRA-13666
**Subsystem**: `src/java/org/apache/cassandra/index/internal/composites/CompositesSearcher.java`
**Category**: Incorrect filtering / wrong query result

---

## Root Cause

`CompositesSearcher` scans index entries and tries to match each entry to a clustering in the base table. When a row inside a partition is deleted, the base table still holds the partition because it has static columns; the clustering key in that case is all nulls (representing the virtual "static row" clustering). The searcher was unconditionally marking such index entries as stale and queuing them for deletion instead of recognising them as valid references to a static-data-only partition. The fix checks whether all clustering values are null and the table has static columns before declaring an entry stale.

### Before / After

```java
// BEFORE
if (cmp == 0)
    return entry;
else
    staleEntries.add(entry);
```

```java
// AFTER
if (cmp == 0)
    return entry;
else {
    if (!dataIter.metadata().hasStaticColumns()
            || !containsOnlyNullValues(indexedEntryClustering))
        staleEntries.add(entry);
}
```

---

## How to Spot

Index searchers that correlate index entries with base-table clusterings must account for the degenerate clustering that represents a static-only partition (all-null clustering key). Any place that assumes "clustering mismatch = stale entry" without checking for the all-null static-row sentinel is vulnerable to silently dropping valid results for partitions that retain only static data after row deletion.

---

## Test

`CassandraIndexTest#testIndexOnPartitionKeyWithPartitionWithoutRows` inserts rows, deletes the clustering row from one partition (leaving only static data), then asserts the index query on `pk2` still returns that partition with `null` clustering values.
