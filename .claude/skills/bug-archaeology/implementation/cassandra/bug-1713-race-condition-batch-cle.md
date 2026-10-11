# Bug 1713: Race Condition in Batch Commit Log Executor

**Commit**: 5ddeba3365 · **Ticket**: CASSANDRA-6860
**Subsystem**: db/commitlog/CommitLogSegment
**Category**: Race condition / missing synchronization

---

## Root Cause

In `CommitLogSegment.sync()`, the `syncComplete` condition was signalled before `lastSyncedOffset` was updated and before `close()` was called. Threads waiting on `syncComplete` would wake up, read the stale `lastSyncedOffset`, and potentially observe an inconsistent state. The fix reorders operations so that `lastSyncedOffset` is assigned and only then `syncComplete.signalAll()` is called, with `close()` deferred until after the signal.

### Before / After

```java
// BEFORE
buffer.force();
syncComplete.signalAll();
if (close)
{
    close();
    nextMarker = buffer.capacity();
}
CLibrary.trySkipCache(fd, offset, nextMarker);
lastSyncedOffset = nextMarker;
```

```java
// AFTER
buffer.force();
if (close)
    nextMarker = buffer.capacity();
lastSyncedOffset = nextMarker;
syncComplete.signalAll();
CLibrary.trySkipCache(fd, offset, nextMarker);
if (close)
    close();
```

---

## How to Spot

When signalling a condition variable, verify that all state that waiting threads will read has already been updated before the signal. Signalling before updating shared state is a classic condition-variable ordering bug.

---

## Test

No test added.
