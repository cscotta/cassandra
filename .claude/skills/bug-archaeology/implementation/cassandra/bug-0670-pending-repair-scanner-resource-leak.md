# Bug 0670: Scanner Resource Leak When SSTable Repair Status Changes During Scan Acquisition

**Commit**: 9c3354e321 · **Ticket**: CASSANDRA-13751
**Subsystem**: src/java/org/apache/cassandra/db/compaction/PendingRepairManager.java · CompactionStrategyManager.java · ISSTableScanner.java
**Category**: Resource leak

---

## Root Cause

`PendingRepairManager.getScanners()` built a set of `ISSTableScanner` instances by iterating over SSTable groups. If an SSTable's repair status changed concurrently (e.g., anticompaction completed between when the caller selected the SSTable and when the scanner was opened), the `getOrCreate()` call would receive a `null` pending-repair UUID and throw an unchecked exception. Any scanners that had already been opened before the throw were not closed, leaking file handles. Additionally, `AbstractCompactionStrategy` and `LeveledCompactionStrategy` duplicated verbose scanner-close-on-error boilerplate. The fix extracts `ISSTableScanner.closeAllAndPropagate()` as a shared utility, wraps the scanner-building loop in try/catch, and retries the entire acquisition on `ConcurrentModificationException` (signaled when the pending-repair ID is null).

### Before / After

```java
// BEFORE — exception during acquisition leaks already-opened scanners
for (Map.Entry<UUID, Set<SSTableReader>> entry : sessionSSTables.entrySet()) {
    scanners.addAll(getOrCreate(entry.getKey()).getScanners(...).scanners);
    // if getOrCreate throws, scanners opened so far are leaked
}
```

```java
// AFTER — opened scanners always closed on error
try {
    for (Map.Entry<UUID, Set<SSTableReader>> entry : sessionSSTables.entrySet()) {
        scanners.addAll(getOrCreate(entry.getKey()).getScanners(...).scanners);
    }
} catch (Throwable t) {
    ISSTableScanner.closeAllAndPropagate(scanners, t);
}
```

---

## How to Spot

Whenever a collection of `Closeable` resources is built in a loop, any exception thrown partway through the loop leaves the already-created resources open. The pattern `list.add(openResource())` inside a loop must be wrapped in a try/catch that closes all list members before rethrowing.

---

## Test

Two new tests `getScannersInvalidSSTable` and `getOrCreateInvalidSSTable` verify that passing a non-pending-repair SSTable throws `IllegalSSTableArgumentException` rather than a bare assertion error or null dereference.
