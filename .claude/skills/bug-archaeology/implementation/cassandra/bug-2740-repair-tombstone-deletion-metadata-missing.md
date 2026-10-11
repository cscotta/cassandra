# Bug 2740: Repair/sstable2json misses row tombstones because deletion metadata gathered after column iteration

**Commit**: 7d44bbd308 · **Ticket**: CASSANDRA-2279

## Detection Rule
When merging column iterators from multiple sources, collect each source's deletion metadata (markedForDeleteAt, localDeletionTime) before or during column collection, not after.

## Code Smell
`RowIteratorFactory` builds `returnCF` from `firstMemtable` only, then iterates all column iterators for their columns without reading each iterator's `getColumnFamily()` deletion metadata, so row tombstones from SSTables or non-first memtables are silently dropped.

## Tags
- scope: cassandra
- category: logic-error
- code-pattern: deletion-metadata-not-propagated
- severity: data-loss
- subsystem: repair

## Root Cause
The merging iterator in `RowIteratorFactory` initialised `returnCF` from the first memtable's column family (or a new empty CF), then computed `markedForDeleteAt` by manually looping over `colIters` after calling `columns.hasNext()`. This approach was fragile and missed deletion metadata from iterators that had already been partially consumed. The fix moves deletion tracking into the `reduce()` method (called once per source) which calls `returnCF.delete(current.getColumnFamily())` immediately on each source CF, propagating all tombstone markers before column merging begins.

## Example
```java
// BEFORE — deletion metadata collected manually after hasNext() calls
returnCF = firstMemtable.getColumnFamily(key) == null
    ? ColumnFamily.create(...) : returnCF.cloneMeShallow();
long lastDeletedAt = Long.MIN_VALUE;
for (IColumnIterator columns : colIters) {
    columns.hasNext();
    if (columns.getColumnFamily().isMarkedForDelete())
        lastDeletedAt = Math.max(lastDeletedAt, ...);
}
returnCF.markedForDeleteAt.set(lastDeletedAt);
```
```java
// AFTER — deletion merged in reduce() before column collection
public void reduce(IColumnIterator current) {
    colIters.add(current);
    this.key = current.getKey();
    this.returnCF.delete(current.getColumnFamily());
}
```
