# Bug 0773: Missing syncComplete Signal When Commit Log Segment Is Recycled

**Commit**: c25fcabe24 · **Ticket**: CASSANDRA-13037
**Subsystem**: Commit log
**Category**: Race condition / missing synchronization

---

## Root Cause
When a commit log segment is recycled (the `close()` path in `CommitLogSegment`), the code called `discardUnusedTail()`, `waitForModifications()`, and set `lastSyncedOffset` to the buffer capacity — marking the segment fully synced — but never called `syncComplete.signalAll()`. Any thread blocked on `syncComplete` waiting to be notified that its mutation had been durably written would therefore wait indefinitely (or until timeout) even though the segment had already been closed and all its data committed.

The fix adds a single `syncComplete.signalAll()` call immediately after `lastSyncedOffset` is updated, so waiters are woken up as soon as the segment is considered fully flushed.

### Before / After
```java
// BEFORE: waiters on syncComplete never unblocked during recycling
discardUnusedTail();
waitForModifications();
lastSyncedOffset = buffer.capacity();
internalClose();

// AFTER: signal added before close
discardUnusedTail();
waitForModifications();
lastSyncedOffset = buffer.capacity();
syncComplete.signalAll();  // wake any threads waiting for sync
internalClose();
```

---

## How to Spot
Any time a condition variable is used to signal "operation X is complete", audit every code path that makes X complete to ensure all of them call `signal` or `signalAll`. It is easy to cover the normal completion path while forgetting an exceptional or cleanup path (here, segment recycling vs. normal periodic sync). A straightforward grep for all assignments to the guarded variable (`lastSyncedOffset`) alongside a search for `syncComplete.signal` will reveal unmatched paths.

---

## Test
No test added (race condition requiring specific timing to reproduce reliably).
