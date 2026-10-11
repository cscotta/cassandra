# Bug 0404: Queries with Guaranteed-Empty Slice Bounds Still Execute

**Commit**: 9bc8d0b452 · **Ticket**: CASSANDRA-16435
**Subsystem**: src/java/org/apache/cassandra/db/filter/ClusteringIndexSliceFilter.java
**Category**: Incorrect filtering / wrong query result

---

## Root Cause

When a CQL query contains contradictory range predicates that produce an empty slice set (e.g., `c2 > 1 AND c2 < 1`), `SelectStatement` would return `ReadQuery.empty()` if `makeClusteringIndexFilter` returned `null`, but it did not check whether the resulting filter was non-null yet logically empty. The `ClusteringIndexSliceFilter` wrapping an empty `Slices` object was handed to `SinglePartitionReadCommand.execute()`, which then forwarded the request to storage nodes instead of short-circuiting. This caused unnecessary network round-trips and could interact incorrectly with read-repair logic, returning wrong row counts during paging.

### Before / After

```java
// BEFORE (SelectStatement.getSinglePartitionReadQuery)
ClusteringIndexFilter filter = makeClusteringIndexFilter(options, columnFilter);
if (filter == null)
    return ReadQuery.empty(table);

// BEFORE (SinglePartitionReadCommand.execute)
public PartitionIterator execute(...) {
    return StorageProxy.read(Group.one(this), consistency, clientState, queryStartNanoTime);
}
```

```java
// AFTER (SelectStatement.getSinglePartitionReadQuery)
ClusteringIndexFilter filter = makeClusteringIndexFilter(options, columnFilter);
if (filter == null || filter.isEmpty(table.comparator))
    return ReadQuery.empty(table);

// AFTER (SinglePartitionReadCommand.execute)
public PartitionIterator execute(...) {
    if (clusteringIndexFilter.isEmpty(metadata().comparator))
        return EmptyIterators.partition();
    return StorageProxy.read(Group.one(this), consistency, clientState, queryStartNanoTime);
}
```

---

## How to Spot

When a filter or range object is constructed from user-supplied predicates, there are two failure points: the factory that builds the filter and the executor that consumes it. Checking only at construction time (null check) misses the case where the filter is non-null but semantically empty. Every `execute` path that accepts a filter should also guard on `isEmpty`, especially when paging or read-repair is involved, since repeated execution of a logically-empty query can produce confusing row-count discrepancies across pages.

---

## Test

`PagingTest.testPagingWithRangeTombstones()` creates a table, inserts a range tombstone and three rows, then pages through a descending query and asserts all three rows are returned in correct order without spurious reads.
