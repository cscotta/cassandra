# Bug 0976: ORDER BY with IN Clause NPE on Null Column Value

**Commit**: deafdbe373 · **Ticket**: CASSANDRA-10955
**Subsystem**: CQL / Query execution
**Category**: Missing null / bounds check

---

## Root Cause
When a `SELECT` with both `ORDER BY` and an `IN` restriction is executed, Cassandra collects result rows and re-sorts them in memory using `SingleColumnComparator` or `CompositeComparator`. Both comparators delegated directly to the underlying `ByteBuffer` type comparator without checking for `null` values in the row's column list. A partition that has no value for the sort column produces a `null` `ByteBuffer`, which the comparator then passes to the type's `compare()` method, causing an NPE.

The fix introduces a shared `ColumnComparator` base class whose `compare(Comparator, ByteBuffer, ByteBuffer)` helper treats `null` as less-than any non-null value (consistent with CQL `NULL` ordering semantics), and both `SingleColumnComparator` and `CompositeComparator` delegate through it.

### Before / After
```java
// BEFORE – SingleColumnComparator
public int compare(List<ByteBuffer> a, List<ByteBuffer> b)
{
    return comparator.compare(a.get(index), b.get(index)); // NPE if either is null
}

// AFTER – shared null-safe helper in ColumnComparator base class
protected final int compare(Comparator<ByteBuffer> comparator, ByteBuffer aValue, ByteBuffer bValue)
{
    if (aValue == null)
        return bValue == null ? 0 : -1;
    return bValue == null ? 1 : comparator.compare(aValue, bValue);
}
```

---

## How to Spot
Any time row results are sorted in the coordinator layer, verify that the comparator handles sparse rows where a non-primary-key column being sorted on may be absent (null). The pattern to watch for is calling an unguarded type comparator directly on a value retrieved from a `List<ByteBuffer>` or a `Row` without a null guard beforehand.

---

## Test
`SelectOrderByTest` gains a new test case that issues an `ORDER BY` query with an `IN` restriction against a table where some rows have no value for the sorted column, asserting that no NPE is thrown and results come back in correct order.
