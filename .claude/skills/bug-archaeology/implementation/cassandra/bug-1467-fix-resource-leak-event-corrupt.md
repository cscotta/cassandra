# Bug 1467: Fix Resource Leak in Event of Corrupt SSTable

**Commit**: 0e83100776 · **Ticket**: CASSANDRA-7932
**Subsystem**: src/java/org/apache/cassandra/db/compaction/
**Category**: Resource leak

---

## Root Cause

When opening compaction scanners, `AbstractCompactionStrategy.getScanners()` created `ICompactionScanner` instances one-by-one. If opening a later scanner threw an exception (e.g. because the SSTable was corrupt), all previously opened scanners were abandoned without being closed, leaking file descriptors. Likewise, `DataTracker.removeUnreadableSSTables()` replaced the live SSTable set without releasing the reference counts of the removed (corrupt) SSTables, preventing them from being deleted.

### Before / After

```java
// BEFORE — scanners opened so far are leaked if an exception is thrown mid-loop
for (SSTableReader sstable : sstables)
    scanners.add(sstable.getScanner(range, limiter));
return scanners;
```

```java
// AFTER — already-opened scanners are closed before re-throwing
try {
    for (SSTableReader sstable : sstables)
        scanners.add(sstable.getScanner(range, limiter));
} catch (Throwable t) {
    new ScannerList(scanners).close();
    throw t;
}
return new ScannerList(scanners);
```

---

## How to Spot

When building a collection of `Closeable` resources in a loop, any exception mid-loop must close the resources already acquired before propagating. The pattern "accumulate then return" without a try/catch around the loop is a reliable indicator of a potential resource leak.

---

## Test

Existing `LeveledCompactionStrategyTest` adjusted; no new dedicated test for the leak scenario.
