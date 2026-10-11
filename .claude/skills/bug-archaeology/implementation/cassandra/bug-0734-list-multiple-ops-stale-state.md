# Bug 0734: Multiple list operations in one UPDATE use stale prefetched state

**Commit**: 5ef8a8b408 · **Ticket**: CASSANDRA-13130
**Subsystem**: CQL / Collections
**Category**: Incorrect filtering / wrong query result

---

## Root Cause
CQL list operations that require knowledge of existing list contents (indexed set, indexed delete, by-value remove) call `UpdateParameters.getPrefetchedList()` to retrieve the current list before applying the change. When a single `UPDATE` statement contained two or more such operations on the same list (e.g., `SET l[0] = 9, l = l - [2]`), each operation fetched the list independently from the original prefetched snapshot. Mutations applied by the first operation were invisible to subsequent operations because `getPrefetchedList` never merged in-flight `ColumnFamily` mutations. The second operation therefore acted on the pre-mutation state, and whichever write had the later timestamp won, silently discarding the other.

The fix merges the already-accumulated `ColumnFamily` mutations (`cf`) with the prefetched snapshot before returning, so each successive operation within the same statement sees the cumulative result of all previous operations.

### Before / After
```java
// BEFORE
public List<Cell> getPrefetchedList(ByteBuffer rowKey, ColumnIdentifier cql3ColumnName)
{
    CQL3Row row = prefetchedLists.get(rowKey);
    if (row == null) return Collections.emptyList();
    List<Cell> cql3List = row.getMultiCellColumn(cql3ColumnName);
    return (cql3List == null) ? Collections.emptyList() : cql3List;
}

// AFTER — merges pending mutations before returning
public List<Cell> getPrefetchedList(ByteBuffer rowKey, ColumnIdentifier cql3ColumnName, ColumnFamily cf)
{
    List<Cell> cql3List = row == null ? Collections.emptyList() : row.getMultiCellColumn(cql3ColumnName);
    if (!cf.isEmpty())
    {
        ColumnFamily currentCf = cf.cloneMe();
        for (Cell c : cql3List) currentCf.addColumn(c);
        CQL3Row.RowIterator it = cfm.comparator.CQL3RowBuilder(cfm, timestamp).group(currentCf.iterator());
        cql3List = it.hasNext() ? it.next().getMultiCellColumn(cql3ColumnName) : null;
    }
    return (cql3List == null) ? Collections.emptyList() : cql3List;
}
```

---

## How to Spot
Any code that caches a pre-read snapshot to support multi-step mutations must refresh (or merge) that snapshot between steps within the same statement. If multiple operations on the same mutable collection are all independently resolved against the original prefetched state, last-write-wins semantics will silently discard earlier in-statement operations. The tell is a `getPrefetchedList`-style method that ignores accumulated in-progress mutations.

---

## Test
`CollectionsTest` received extensive new cases: `testMultipleOperationOnListWithinTheSameQuery`, `testMultipleOperationOnMapWithinTheSameQuery`, and `testMultipleOperationOnSetWithinTheSameQuery`, each verifying that compound `UPDATE` statements combining appends, deletes, and indexed sets on the same collection produce the correct merged result.
