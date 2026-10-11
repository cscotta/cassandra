# Bug 0820: Time-Order Query Optimization Applied to Non-Frozen UDTs and Frozen Collections

**Commit**: 21d8a7d3bd · **Ticket**: CASSANDRA-12605
**Subsystem**: Storage / Read Path
**Category**: Incorrect filtering / wrong query result

---

## Root Cause
`SinglePartitionReadCommand` contained an optimization that reads SSTables in reverse timestamp order and short-circuits as soon as all queried cells are found, bypassing older SSTables. The guard condition for this optimization used `queryNeitherCountersNorCollections()`, which tested `column.type.isCollection()`. This check returned `false` for frozen collections (they are not "collection" in the multi-cell sense) but also `true` for non-frozen UDTs (which were not considered). The correct predicate is `column.type.isMultiCell()`: a type is multi-cell when it can spread cells across multiple SSTables — which covers non-frozen collections AND non-frozen UDTs but correctly excludes frozen collections. Applying the optimization to non-frozen UDTs could silently drop UDT fields stored in older SSTables.

### Before / After
```java
// BEFORE — method and callee
if (clusteringIndexFilter() instanceof ClusteringIndexNamesFilter
        && queryNeitherCountersNorCollections())

private boolean queryNeitherCountersNorCollections() {
    for (ColumnDefinition column : columnFilter().fetchedColumns()) {
        if (column.type.isCollection() || column.type.isCounter())
            return false;
    }
    return true;
}

// AFTER — method renamed and logic inverted
if (clusteringIndexFilter() instanceof ClusteringIndexNamesFilter
        && !queriesMulticellType())

private boolean queriesMulticellType() {
    for (ColumnDefinition column : columnFilter().fetchedColumns()) {
        if (column.type.isMultiCell() || column.type.isCounter())
            return true;
    }
    return false;
}
```

---

## How to Spot
Whenever an SSTable-skipping optimization is gated on column type, verify that the type predicate covers every multi-cell type: non-frozen collections, non-frozen UDTs, and counters. `isCollection()` misses UDTs; `isMultiCell()` is the correct query for "can spread across SSTables." Review the rename carefully: the old double-negative `!queryNeitherCounters...` was easy to mis-read; the new `!queriesMulticellType()` at the call site is clearer.

---

## Test
No new test was added; only the predicate and its documentation comment were changed.
