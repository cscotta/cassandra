# Bug 0637: CompactionStrategyManager Acquires Read Lock on Every shouldDefragment/getLevelFanoutSize Call

**Commit**: f3cf1c019e · **Ticket**: CASSANDRA-13930
**Subsystem**: `db/compaction/CompactionStrategyManager.java`
**Category**: Race condition / missing synchronization

---

## Root Cause

`shouldDefragment()` and `getLevelFanoutSize()` each acquired a `ReadWriteLock` read lock, accessed the first compaction strategy in `repaired`, and released the lock. These methods can be called on every read path to decide whether to perform read-time defragmentation. The lock acquisition was unnecessarily heavyweight: both values are immutable for the lifetime of a given compaction strategy configuration and only change when the strategy is replaced. The fix caches the values in `volatile`-equivalent plain fields updated inside the existing write-locked `startup()` path, making every subsequent read lock-free.

### Before / After

```java
// BEFORE — lock acquired on every read-path call
public boolean shouldDefragment() {
    readLock.lock();
    try {
        assert repaired.get(0).getClass().equals(unrepaired.get(0).getClass());
        return repaired.get(0).shouldDefragment();
    } finally {
        readLock.unlock();
    }
}
```

```java
// AFTER — value cached at startup; lock-free read
private boolean shouldDefragment;

// set once inside write-locked startup():
shouldDefragment = repaired.get(0).shouldDefragment();

public boolean shouldDefragment() {
    return shouldDefragment;
}
```

---

## How to Spot

When a method acquires a lock only to read a value that changes at most on strategy reconfiguration (a rare event), caching the value at reconfiguration time eliminates repeated lock contention on the hot path. Reviewers should ask: "Does this value actually change during normal operation? If not, can we cache it?"

---

## Test

No test added.
