# Bug 2921: Race condition in getRangeRows — current memtable excluded from range scan

**Commit**: 7ecd33ace4 · **Ticket**: CASSANDRA-1040

## Detection Rule
When a range scan collects live memtable data separately from pending-flush memtables but passes the live memtable without proper thread-safe access, recently written rows are either missed or read without locking.

## Code Smell
`RowIteratorFactory.getIterator` takes both a `current` memtable (accessed with locks) and a `memtables` collection (pending flush); callers that omit the current memtable from the flush list will miss unflushed rows in range queries.

## Tags
- scope: cassandra
- category: race-condition
- code-pattern: missing-memtable-in-scan
- severity: silent-wrong-result
- subsystem: storage-engine

## Root Cause
`getRangeRows` built the memtable list from `memtablesPendingFlush` only, then passed the current memtable as a separate argument to `RowIteratorFactory.getIterator` where it was accessed with a lock. The lock path was error-prone and the current memtable could be missed. The fix retrieves the current memtable via the thread-safe `getMemtableThreadSafe()` method and adds it to the front of the list, removing the need for a separate `current` parameter.

## Example
```java
// BEFORE
Collection<Memtable> memtables = new ArrayList<Memtable>(memtablesPendingFlush);
// current memtable passed separately, accessed with locks inside factory
RowIterator iterator = RowIteratorFactory.getIterator(
    memtable_, memtables, sstables, ...);
```
```java
// AFTER
Collection<Memtable> memtables = new ArrayList<Memtable>();
memtables.add(getMemtableThreadSafe());  // current first, safe access
memtables.addAll(memtablesPendingFlush);
RowIterator iterator = RowIteratorFactory.getIterator(
    memtables, sstables, ...);
```
