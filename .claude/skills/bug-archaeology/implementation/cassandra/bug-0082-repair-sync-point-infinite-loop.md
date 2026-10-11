# Bug 0082: Infinite Loop and Missing Progress-Log Notification for Sync-Point Durability

**Commit**: a5dc44ad57 · **Ticket**: CASSANDRA-20125
**Subsystem**: service/accord/repair/RepairSyncPointAdapter
**Category**: Logic error in condition

---

## Root Cause

`RepairSyncPointAdapter.execute` used `ExecuteSyncPoint.ExecuteBlocking`, which waits for a sync point to be *applied* before notifying its callback. Under certain conditions this created an infinite loop: the repair coordinator was waiting for the sync point to become durable, but the sync point itself was waiting for the coordinator's progress-log notification in order to proceed. Switching to `ExecuteInclusive` — which notifies the progress log *while* waiting to apply, rather than only after — breaks the cycle and allows forward progress.

### Before / After

```java
// BEFORE
ExecuteSyncPoint.ExecuteBlocking<U> execute = new ExecuteSyncPoint.ExecuteBlocking<>(
    node, new SyncPoint<>(txnId, deps, (FullRoute<U>) route), tracker, executeAt);
```

```java
// AFTER
ExecuteSyncPoint.ExecuteInclusive<U> execute = new ExecuteSyncPoint.ExecuteInclusive<>(
    node, new SyncPoint<>(txnId, deps, (FullRoute<U>) route), tracker, executeAt);
```

---

## How to Spot

When a coordinator waits on a future that is itself gated on a notification the coordinator is responsible for sending, the result is a deadlock or infinite loop. During code review, trace every "wait until X" path and verify that the required precondition for X can be satisfied without the waiting thread taking further action.

---

## Test

No test added (the accord submodule pointer was also advanced).
