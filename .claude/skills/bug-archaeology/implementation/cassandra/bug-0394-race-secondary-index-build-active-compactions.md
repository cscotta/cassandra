# Bug 0394: Race Between Secondary Index Building and Active Compactions Tracking

**Commit**: a1285ac92d · **Ticket**: CASSANDRA-16554
**Subsystem**: src/java/org/apache/cassandra/io/sstable / db/compaction
**Category**: Race condition / missing synchronization

---

## Root Cause

`KeyIterator`, `ReducingKeyIterator`, `Scrubber`, and `Verifier` all expose a `getCompactionInfo()` method that reads position data (e.g., `dataFile.getFilePointer()`) from a `RandomAccessReader` which is explicitly annotated `@NotThreadSafe`. Meanwhile `CompactionManager.active.getCompactionsForSSTable()` can query `getCompactionInfo()` from a different thread while the index builder thread is iterating and potentially closing those readers. With no synchronization the concurrent read of a closed or partially-updated reader caused undefined behaviour (NPE, stale position, or use-after-close). The fix introduces a `ReentrantReadWriteLock` around every reader access: the read lock is held during iteration and `getCompactionInfo()`, and the write lock is held during `close()`, ensuring the reader is never closed while another thread is reading its position.

### Before / After

```java
// BEFORE – KeyIterator.close() and getBytesRead() unsynchronized
public void close()  { in.close(); }
public long getBytesRead() { return in.getFilePointer(); }
```

```java
// AFTER – guarded by a ReadWriteLock
public void close() {
    fileAccessLock.writeLock().lock();
    try { in.close(); }
    finally { fileAccessLock.writeLock().unlock(); }
}
public long getBytesRead() {
    fileAccessLock.readLock().lock();
    try { return in.getFilePointer(); }
    finally { fileAccessLock.readLock().unlock(); }
}
```

---

## How to Spot

Any time a `Closeable` object that is `@NotThreadSafe` is accessed from two independent code paths (one performing iteration, another querying progress state), a `ReadWriteLock` (or equivalent) is required. During review, look for `CompactionInfo.Holder` implementations whose `getCompactionInfo()` reads mutable fields shared with `close()`, then verify that all accessors are synchronized against concurrent close.

---

## Test

`ActiveCompactionsTest.testActiveCompactionTrackingRaceWithIndexBuilder` hammers 5,000 iterations of concurrent index builds and `getCompactionsForSSTable()` calls from two threads, verifying no exception is thrown.
