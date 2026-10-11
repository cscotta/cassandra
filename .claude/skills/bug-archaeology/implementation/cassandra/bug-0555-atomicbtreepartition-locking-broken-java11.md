# Bug 0555: AtomicBTreePartition Locking Broken on Java 11

**Commit**: 9063ceabc9 · **Ticket**: CASSANDRA-14607
**Subsystem**: `src/java/org/apache/cassandra/db/partitions/AtomicBTreePartition.java`
**Category**: Race condition / missing synchronization

---

## Root Cause

The original design used a version-split build: `src/java8` contained an `AtomicBTreePartitionBase` that used `sun.misc.Unsafe.monitorEnter/monitorExit` for pessimistic locking, while `src/java11` provided a `ReentrantLock`-based replacement. The fallback logic in `addAllWithSizeDelta` acquired the lock before the retry loop but only held a `monitorOwned` boolean that was set before calling `acquireLock`. Under Java 11, the `ReentrantLock` path had a subtle structural issue where the lock could be acquired once and then re-entered on the next loop iteration without being released, and the two-file split made it impossible to maintain correctly as a unit. The fix eliminates the split entirely: it removes both version-specific base classes and inlines a plain `synchronized (this)` block directly inside the retry loop in `AtomicBTreePartition`, removing all `Unsafe` and `ReentrantLock` machinery.

### Before / After

```java
// BEFORE (Java 8 base, src/java8)
protected final void acquireLock() {
    if (unsafe != null) unsafe.monitorEnter(this);
}
// AtomicBTreePartition extends AtomicBTreePartitionBase
// acquireLock() called outside while(true) loop
```

```java
// AFTER (no base class, inline synchronization)
if (shouldLock) {
    synchronized (this) {
        long[] result = addAllWithSizeDeltaInternal(updater, update, indexer);
        if (result != null) return result;
    }
} else {
    long[] result = addAllWithSizeDeltaInternal(updater, update, indexer);
    if (result != null) return result;
}
```

---

## How to Spot

When a class uses version-specific source directories to provide platform-specific locking primitives, treat the whole locking path as high-risk. Specifically, check that lock acquisition and release are symmetric and that re-entry across loop iterations is impossible. Using `Unsafe.monitorEnter` outside a `synchronized` block is a red flag because the JVM's structured-locking verifier cannot catch mistakes.

---

## Test

No test added.
