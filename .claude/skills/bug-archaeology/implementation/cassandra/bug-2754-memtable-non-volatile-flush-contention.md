# Bug 2754: Memtable Field Read Without Visibility Guarantee Causes Stale Reads

**Commit**: b065aac226 · **Ticket**: CASSANDRA-1954

## Detection Rule
When a field is read by multiple threads and swapped atomically, declare it `volatile` (or protect with a lock) so that readers always see the latest reference without acquiring a read-write lock on the hot write path.

## Code Smell
`private Memtable memtable` is a non-volatile, non-final field read inside `apply` (write path) via `getMemtableThreadSafe()`, which previously acquired `Table.flusherLock.readLock()` on every write just to ensure visibility.

## Tags
- scope: cassandra
- category: race-condition
- code-pattern: non-volatile-field-lock-workaround
- severity: performance
- subsystem: storage-engine

## Root Cause
`ColumnFamilyStore.memtable` was guarded by a `ReadWriteLock` (`Table.flusherLock`) to guarantee visibility across threads. Every write had to acquire the read lock, contending with flush operations that held the write lock. The fix makes `memtable` `volatile`, eliminating the need for a read lock on the write path and replacing the write lock with a plain `ReentrantLock` only during the actual memtable switch.

## Example
```java
// BEFORE
private Memtable memtable;
// ...
private Memtable getMemtableThreadSafe() {
    Table.flusherLock.readLock().lock();
    try { return memtable; }
    finally { Table.flusherLock.readLock().unlock(); }
}
```
```java
// AFTER
private volatile Memtable memtable;
// ...
private Memtable getMemtableThreadSafe() {
    return memtable; // volatile read provides happens-before
}
```
