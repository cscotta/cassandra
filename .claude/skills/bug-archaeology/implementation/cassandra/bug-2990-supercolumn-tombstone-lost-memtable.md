# Bug 2990: SuperColumn tombstone lost when reading from Memtable — deletion not propagated to caller

**Commit**: e181fdc4e0 · **Ticket**: CASSANDRA-101

## Detection Rule
When copying a sub-column out of a super-column in a Memtable, always wrap it in a new SuperColumn container that carries the parent's tombstone metadata, so range tombstones are not silently dropped.

## Code Smell
A sub-column is copied into the result `ColumnFamily` by constructing a flat column string ("superCol:subCol") and calling `addColumn` with its raw bytes, discarding the parent super-column's `markedForDeleteAt` and `localDeletionTime`.

## Tags
- scope: cassandra
- category: state-not-cleaned
- code-pattern: tombstone-dropped-on-copy
- severity: silent-wrong-result
- subsystem: storage-engine

## Root Cause
`Memtable.getLocalCopy()` extracted a sub-column and reconstructed it as a plain column string, ignoring the parent `SuperColumn`'s deletion markers. Readers therefore never saw super-column tombstones from unflushed data, causing deleted super-columns to reappear as live data. The fix creates a new `SuperColumn` container, propagates the deletion metadata with `markForDeleteAt()`, adds the sub-column to it, and then adds the container to the result `ColumnFamily`. Additionally, `SuperColumn.putColumn()` was fixed to call `markForDeleteAt()` instead of only updating `markedForDeleteAt`, ensuring `localDeletionTime` is also preserved.

## Example
```java
// BEFORE: tombstone metadata silently dropped
columnFamily.addColumn(
    values[1] + ":" + values[2],
    subColumn.value(), subColumn.timestamp(), subColumn.isMarkedForDelete());

// AFTER: wrap in SuperColumn carrying deletion metadata
SuperColumn container = new SuperColumn(superColumn.name());
container.markForDeleteAt(superColumn.getLocalDeletionTime(),
                          superColumn.getMarkedForDeleteAt());
container.addColumn(subColumn.name(), subColumn);
columnFamily.addColumn(container);
```
