# Bug 2307: Index SSTable cleanup uses global partitioner to estimate ranges, causing incorrect compaction estimates

**Commit**: be969899c9 · **Ticket**: CASSANDRA-4403

## Detection Rule

When a method branches on `OperationType.CLEANUP` to use token-range-based size estimation, check whether it also applies that logic to index SSTables, which use a different (local) partitioner and cannot use the global `StorageService.getLocalRanges()`.

## Code Smell

A `getExpectedCompactedFileSize` method that calls `StorageService.instance.getLocalRanges(table.name)` and uses the result for range position lookups on SSTables without excluding index SSTables (`isIndex()`).

## Tags

- scope: cassandra
- category: wrong-filter-result
- code-pattern: missing-index-sstable-guard
- severity: silent-wrong-result
- subsystem: compaction

## Root Cause

`ColumnFamilyStore.getExpectedCompactedFileSize()` used the global partitioner's token ranges (via `StorageService.getLocalRanges`) to estimate the expected post-compaction size during CLEANUP. However, secondary-index SSTables use a different internal partitioner whose tokens are not comparable to the main table's token ranges. Calling `sstable.getPositionsForRanges(ranges)` on an index SSTable returned wrong positions, leading to incorrect size estimates that could affect compaction throttling and scheduling.

## Example

```java
// BEFORE
public long getExpectedCompactedFileSize(Iterable<SSTableReader> sstables, OperationType operation)
{
    long expectedFileSize = 0;
    if (operation == OperationType.CLEANUP)
    {
        Collection<Range<Token>> ranges = StorageService.instance.getLocalRanges(table.name);
        for (SSTableReader sstable : sstables)
        {
            List<Pair<Long, Long>> positions = sstable.getPositionsForRanges(ranges); // wrong for indexes
            for (Pair<Long, Long> position : positions)
                expectedFileSize += position.right - position.left;
        }
    }
    // ...
}
```

```java
// AFTER
public long getExpectedCompactedFileSize(Iterable<SSTableReader> sstables, OperationType operation)
{
    if (operation != OperationType.CLEANUP || isIndex())
        return SSTable.getTotalBytes(sstables);

    long expectedFileSize = 0;
    Collection<Range<Token>> ranges = StorageService.instance.getLocalRanges(table.name);
    for (SSTableReader sstable : sstables)
    {
        List<Pair<Long, Long>> positions = sstable.getPositionsForRanges(ranges);
        for (Pair<Long, Long> position : positions)
            expectedFileSize += position.right - position.left;
    }
    return expectedFileSize;
}
```
