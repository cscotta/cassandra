# Bug 1561: Fix Race Condition During Pending Range Calculation

**Commit**: ae03e1bab7 · **Ticket**: CASSANDRA-7390
**Subsystem**: locator / TokenMetadata, service / PendingRangeCalculatorService
**Category**: Race condition / missing synchronization

---

## Root Cause

Pending range calculation was split across two classes in a way that introduced a TOCTOU race: `PendingRangeCalculatorService` read token metadata fields (bootstrap tokens, leaving endpoints, etc.) without holding the `TokenMetadata` read lock, then called `setPendingRanges` to store the result. A concurrent topology change between the read and the write could leave an internally inconsistent pending-ranges map. Additionally, `blockUntilFinished()` polled `getActiveCount() + getPendingTasks()`, which can transiently reach zero between task submission and thread pickup, causing callers to return prematurely. The fix moves the entire calculation inside `TokenMetadata.calculatePendingRanges()` where it runs under the existing read lock, and replaces the unreliable poll with an `AtomicInteger` job counter that is incremented on submit and decremented only after the task body completes (or is rejected).

### Before / After

```java
// BEFORE – calculation outside the lock, result stored via separate setter
public void setPendingRanges(String keyspaceName,
        Multimap<Range<Token>, InetAddress> rangeMap) {
    pendingRanges.put(keyspaceName, rangeMap);
}

// blockUntilFinished races: count can hit 0 before task starts
while (true) {
    if (executor.getActiveCount() + executor.getPendingTasks() == 0)
        break;
    ...
}
```

```java
// AFTER – calculation runs inside TokenMetadata under its own read lock
public void calculatePendingRanges(AbstractReplicationStrategy strategy,
        String keyspaceName) {
    lock.readLock().lock();
    try {
        ...
        pendingRanges.put(keyspaceName, newPendingRanges);
    } finally {
        lock.readLock().unlock();
    }
}

// blockUntilFinished uses an AtomicInteger incremented before submit,
// decremented after task body (or on rejection)
while (updateJobs.get() > 0) { Thread.sleep(100); }
```

---

## How to Spot

When a "calculate then store" pattern spans multiple methods across classes, verify that the entire operation is performed under the same lock. Also check that thread-pool "finished" detection accounts for the window between `submit()` returning and the task actually starting execution — `getActiveCount()` alone is insufficient.

---

## Test

`MoveTest` was updated with a `@Before` that calls `blockUntilFinished()` and clears token metadata before each test, and an explicit `blockUntilFinished()` call was inserted between successive `onChange()` bootstrap events to prevent interleaving.
