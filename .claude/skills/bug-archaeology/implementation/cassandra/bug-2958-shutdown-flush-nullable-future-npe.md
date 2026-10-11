# Bug 2958: NPE in shutdown hook when forceFlush() returns null

**Commit**: 76d625d509 · **Ticket**: CASSANDRA-2958

## Detection Rule
When collecting `Future<?>` objects returned by `cfs.forceFlush()`, null-check each future before adding it to the wait list because `forceFlush()` can return null when there is nothing to flush.

## Code Smell
`flushes.add(cfs.forceFlush())` without a null check will add a null entry to the futures list, causing a NullPointerException in `FBUtilities.waitOnFutures()`.

## Tags
- scope: cassandra
- category: missing-null-check
- code-pattern: nullable-future-added-to-wait-list
- severity: crash
- subsystem: bootstrap-startup

## Root Cause
The shutdown hook in `StorageService` called `cfs.forceFlush()` and directly added the result to a list of futures to wait on. `forceFlush()` returns null when the memtable is already empty and no flush is needed. `FBUtilities.waitOnFutures()` then NPEs on the null entry. The fix stores the result in a local variable and only adds it to the list when non-null.

## Example
```java
// BEFORE
for (ColumnFamilyStore cfs : table.getColumnFamilyStores())
    flushes.add(cfs.forceFlush());
```
```java
// AFTER
for (ColumnFamilyStore cfs : table.getColumnFamilyStores()) {
    Future<?> future = cfs.forceFlush();
    if (future != null)
        flushes.add(future);
}
```
