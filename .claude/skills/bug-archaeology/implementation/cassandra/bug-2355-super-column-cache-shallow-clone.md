# Bug 2355: Super column row cache stores shallow clone, preserving reference to mutable sub-column data

**Commit**: eb9f96146e · **Ticket**: CASSANDRA-4190

## Detection Rule

When caching a `SuperColumn`, verify that the cached copy is a deep clone (`cloneMe()`) rather than a shallow clone (`cloneMeShallow()`) — a shallow clone shares references to the same sub-column data and will reflect mutations made after caching.

## Code Smell

`columns.addColumn(((SuperColumn)c).cloneMeShallow(), allocator)` in the row-cache path passes a shallow copy that still holds references to live `IColumn` children; any subsequent write to those sub-columns silently corrupts the cached view.

## Tags

- scope: cassandra
- category: state-not-cleaned
- code-pattern: shallow-clone-for-cache
- severity: silent-wrong-result
- subsystem: storage-engine

## Root Cause

`ColumnFamily.addAllWithSCCopy()` was designed to break references to live `SuperColumn` structures when populating the row cache. It called `cloneMeShallow()` which copies the `SuperColumn` wrapper but shares the underlying `IColumn` children map. A subsequent write to a sub-column in the live structure would therefore be visible through the cache, bypassing the intended isolation. Changing the call to `cloneMe()` (deep clone) ensures the cached super column and its children are fully independent copies.

## Example

```java
// BEFORE
for (IColumn c : cf) {
    columns.addColumn(((SuperColumn)c).cloneMeShallow(), allocator); // shares sub-columns
}
```

```java
// AFTER
for (IColumn c : cf) {
    columns.addColumn(((SuperColumn)c).cloneMe(), allocator); // deep copy
}
```
