# Bug 2724: sstablesToIterate counter incremented outside the if-block, inflating SSTable read stats

**Commit**: 9c9e79b357 · **Ticket**: CASSANDRA-2422

## Detection Rule
When a counter should track how many items pass a conditional branch, ensure the increment is inside that branch, not after it.

## Code Smell
`sstablesToIterate++` appears after the closing brace of the `if (sstable != null)` block, so it fires for every SSTable examined, not only the ones that actually provide data.

## Tags
- scope: cassandra
- category: logic-error
- code-pattern: counter-outside-branch
- severity: silent-wrong-result
- subsystem: storage-engine

## Root Cause
The read-path code iterates over SSTables and only adds an iterator to the `iterators` list when the SSTable has data for the requested key. The SSTable read-count metric `sstablesToIterate` is supposed to reflect this subset, but it was incremented unconditionally (outside the `if` block), counting all examined SSTables instead of just the contributing ones. This caused `recentSSTablesPerRead` metrics to be inflated.

## Example
```java
// BEFORE — counter outside the if block
if (sstable != null) {
    returnCF.delete(iter.getColumnFamily());
    iterators.add(iter);
}
sstablesToIterate++;  // always runs
```
```java
// AFTER — counter inside the if block
if (sstable != null) {
    returnCF.delete(iter.getColumnFamily());
    iterators.add(iter);
    sstablesToIterate++;  // only when SSTable contributes
}
```
