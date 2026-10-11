# Bug 3110: SSTable iterators closed before use in CollationController

**Commit**: f0a2946434 · **Ticket**: CASSANDRA-3110

## Detection Rule
When a `try/finally` block closes resources in the `finally` clause and then the return statement follows *outside* the try block, the iterators are closed before the caller can consume them.

## Code Smell
Code that calls `FileUtils.closeQuietly(iter)` in a `finally` block and then returns a result that wraps or references those same iterators outside the `finally` scope will hand the caller already-closed iterators.

## Tags
- scope: cassandra
- category: resource-leak
- code-pattern: return-after-close-in-finally
- severity: crash
- subsystem: storage-engine

## Root Cause
`CollationController.collectTimeOrderedData()` and `collectAllData()` had their `return` statements placed *after* the `try/finally` block, meaning the `finally` clause would close all SSTable iterators before the constructed `ColumnFamily` was returned to the caller. The fix moved the return statement inside the `try` block so iterators are only closed after the data has been fully read and collated.

## Example
```java
// BEFORE
try {
    // read iterators into container
} finally {
    for (IColumnIterator iter : iterators)
        FileUtils.closeQuietly(iter);
}
// iterators already closed here!
filter.collateColumns(returnCF, iterators, ...);
return returnCF;
```
```java
// AFTER
try {
    // read iterators into container
    filter.collateColumns(returnCF, iterators, ...);
    return returnCF;
} finally {
    for (IColumnIterator iter : iterators)
        FileUtils.closeQuietly(iter);
}
```
