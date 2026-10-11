# Bug 1990: Range Tombstones Skipped During Reverse Queries in IndexedSliceReader

**Commit**: 1a8f7230a1 · **Ticket**: CASSANDRA-5712
**Subsystem**: src/java/org/apache/cassandra/db/columniterator/IndexedSliceReader.java
**Category**: Incorrect filtering / wrong query result

---

## Root Cause

`IndexedSliceReader.addColumn()` stored all atoms for a block by prepending them (`blockColumns.addFirst(col)`) during reverse iteration. Range tombstone markers are recorded at the start of the range they cover, which in forward order comes before the columns they delete. When reversed, these tombstones were prepended into the same `blockColumns` deque as regular columns, meaning they ended up interleaved in a position where the caller saw live columns before seeing the tombstone that should have deleted them. As a result, columns that should have been considered deleted were returned as live.

The fix introduces a separate `rangeTombstonesReversed` deque. In `addColumn()`, `RangeTombstone` atoms are placed in this dedicated deque (prepended to maintain reverse order), while regular columns go into `blockColumns` as before. In `computeNext()`, all queued tombstones from the dedicated deque are drained first before any regular column for that block, ensuring the caller always sees tombstones before the columns they cover.

### Before / After

```java
// BEFORE: tombstones and columns intermixed in reverse order
protected void addColumn(OnDiskAtom col)
{
    if (reversed)
        blockColumns.addFirst(col); // tombstone ends up after columns it covers
    else
        blockColumns.addLast(col);
}
```

```java
// AFTER: tombstones drained first per block in reverse mode
protected void addColumn(OnDiskAtom col)
{
    if (reversed)
    {
        if (col instanceof RangeTombstone)
            rangeTombstonesReversed.addFirst(col);
        else
            blockColumns.addFirst(col);
    }
    else
    {
        blockColumns.addLast(col);
    }
}
```

---

## How to Spot

When reversing iteration order over a sequence that contains out-of-band metadata (tombstones, markers) that must precede the data they annotate, simply reversing the insertion order is not sufficient — the metadata must still appear before the data it covers in the output stream, regardless of iteration direction.

---

## Test

A new `reverseQueryTest()` was added to `RangeTombstoneTest` that flushes two SSTables (one with data, one with a range tombstone that covers part of the data), then issues a reverse slice query with `LIMIT 1` and asserts the correct last surviving column is returned.
