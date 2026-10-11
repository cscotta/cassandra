# Bug 2998: Compaction FileStruct comparator ignores partitioner, breaking ordered merge

**Commit**: 732695503b · **Ticket**: CASSANDRA-80

## Detection Rule
When comparing SSTable keys during compaction merge, use the table's partitioner comparator rather than natural String ordering, as decorated keys may not sort lexicographically by their token order.

## Code Smell
`FileStruct.compareTo()` calls `key.compareTo(f.key)` using plain String comparison rather than consulting the `IPartitioner`, so the priority queue driving the compaction merge is ordered by raw string bytes instead of by token space.

## Tags
- scope: cassandra
- category: wrong-filter-result
- code-pattern: string-compare-instead-of-partitioner
- severity: silent-wrong-result
- subsystem: compaction

## Root Cause
`FileStruct` was constructed without a reference to the `IPartitioner`, so its `compareTo()` method used `String.compareTo()` on decorated keys. For random partitioners (and any partitioner whose key ordering differs from lexicographic byte order), the compaction priority queue was ordering SSTables incorrectly, producing a mis-sorted merged SSTable. The fix adds an `IPartitioner` field to `FileStruct`, passes `StorageService.getPartitioner()` at the construction site, and delegates comparison to `partitioner.getDecoratedKeyComparator()`.

## Example
```java
// BEFORE: lexicographic string order, wrong for non-byte-order partitioners
public int compareTo(FileStruct f) {
    return key.compareTo(f.key);
}

// AFTER: uses partitioner's key ordering
public int compareTo(FileStruct f) {
    return partitioner.getDecoratedKeyComparator().compare(key, f.key);
}
```
