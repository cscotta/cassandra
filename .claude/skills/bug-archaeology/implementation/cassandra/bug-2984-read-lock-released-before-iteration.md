# Bug 2984: Read lock released before SSTable iteration, exposing snapshot to compaction deletion

**Commit**: 5886b7c9ac · **Ticket**: CASSANDRA-161

## Detection Rule
When a read thread snapshots a shared collection under a lock but then releases the lock before iterating the snapshot, it may read from files that a concurrent writer has since deleted.

## Code Smell
A lock is acquired to copy a mutable collection into a local list, then released before the list is iterated — so the iteration executes without holding the lock, racing with the writer that modifies the collection and deletes the underlying resources.

## Tags
- scope: cassandra
- category: race-condition
- code-pattern: lock-released-before-iteration
- severity: silent-wrong-result
- subsystem: storage-engine

## Root Cause
`getColumnFamilyFromDisk` copied `ssTables_` into a local `files` list, released the read lock, then iterated `files`. Between unlock and iteration, the compactor thread could compact those SSTables and delete them. The read thread would then skip the key entirely (false negative) since the new merged SSTable was not in the snapshot. The fix keeps the read lock held for the entire iteration, and also sorts `ssTables_` by switching to a `TreeSet` with a descending comparator so sorting is free.

## Example
```java
// BEFORE: lock released before iteration — race window
lock_.readLock().lock();
try { files.addAll(ssTables_); }
finally { lock_.readLock().unlock(); }
// compactor can delete files here!
for (String file : files) { ... }

// AFTER: lock held for entire iteration
lock_.readLock().lock();
try {
    for (String file : ssTables_) { ... }
} finally {
    lock_.readLock().unlock();
}
```
