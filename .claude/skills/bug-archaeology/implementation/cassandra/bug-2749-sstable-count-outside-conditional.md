# Bug 2749: sstables-per-Read Counter Incremented Outside Conditional

**Commit**: 8b441c4c28 · **Ticket**: n/a

## Detection Rule
When a counter should only be incremented when a condition (e.g., iterator successfully added) is true, verify the increment statement is inside the conditional block, not after the closing brace.

## Code Smell
`sstablesToIterate++` appears one line after the closing brace of the `if (iter.getColumnFamily() != null)` block, making it unconditional; it should be inside the block.

## Tags
- scope: cassandra
- category: off-by-one
- code-pattern: counter-outside-conditional
- severity: performance
- subsystem: storage-engine

## Root Cause
A refactoring moved the `sstablesToIterate++` increment outside the `if` block that checks whether an iterator's column family is non-null. The counter was therefore incremented for every SSTable visited (including those with no matching data), inflating the "sstables per read" metric and incorrectly sizing the merge iterator. The fix moves the increment back inside the conditional.

## Example
```java
// BEFORE
if (iter.getColumnFamily() != null) {
    returnCF.delete(iter.getColumnFamily());
    iterators.add(iter);
}
sstablesToIterate++;  // always runs
```
```java
// AFTER
if (iter.getColumnFamily() != null) {
    returnCF.delete(iter.getColumnFamily());
    iterators.add(iter);
    sstablesToIterate++;  // only when iterator is used
}
```
