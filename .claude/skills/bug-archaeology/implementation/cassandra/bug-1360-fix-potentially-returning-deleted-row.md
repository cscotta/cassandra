# Bug 1360: Fix potentially returning deleted row with range tombstones

**Commit**: 2424dd133b · **Ticket**: CASSANDRA-8558
**Subsystem**: src/java/org/apache/cassandra/db/columniterator/IndexedSliceReader.java
**Category**: Incorrect filtering / wrong query result

---

## Root Cause

`IndexedSliceReader` processes multiple slices in reverse order using an on-disk indexed structure. When a `RangeTombstone` covered a slice that had already been advanced past, the reader failed to emit it, because the code's "before slice start" path dropped any atom whose column name was before the current slice's start — regardless of whether that atom was a `RangeTombstone` whose open bound still reached into the active slice. A deleted row's tombstone marker was therefore silently discarded, allowing the row's live column data (fetched from an earlier block) to survive without its covering deletion.

### Before / After

```java
// BEFORE — tombstone treated identically to regular columns; discarded when before slice start
if (isColumnBeforeSliceStart(column))
{
    if (reversed)
    {
        prefetched.addFirst(column);
    }
    column = null;
}
```

```java
// AFTER — RangeTombstone included unless its max end is also before the slice start
if (column instanceof RangeTombstone && !isBeforeSliceStart(((RangeTombstone)column).max))
{
    addColumn(column);
}
else if (reversed)
{
    prefetched.addFirst(column);
}
column = null;
```

---

## How to Spot

Any place that filters out atoms based solely on a column-name position check, without separately handling `RangeTombstone` (which has a *range*, not a point), is a candidate for this class of bug. When reviewing slice-iterator logic, verify that the guard condition is applied to both the open and the close bound of a range tombstone before discarding it; using `instanceof RangeTombstone` to special-case span-based entries is the idiomatic signal that the code is aware of this distinction.

---

## Test

No test added in this commit; the ticket references manual reproduction steps.
