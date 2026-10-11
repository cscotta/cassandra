# Bug 1600: Fix NPE in StreamTransferTask.createMessageForRetry()

**Commit**: 886d9fce46 · **Ticket**: CASSANDRA-7323
**Subsystem**: src/java/org/apache/cassandra/streaming
**Category**: Missing null / bounds check

---

## Root Cause

`StreamTransferTask.createMessageForRetry()` called `future.cancel(false)` on the result of `timeoutTasks.get(sequenceNumber)` without checking whether the map contained an entry for that sequence number. If a retry arrived for a sequence number that had no scheduled timeout task, `get()` returned `null`, causing a `NullPointerException`. The method was also not `synchronized`, creating a concurrent access risk given that the `timeoutTasks` map is shared across threads.

### Before / After

```java
// BEFORE
public OutgoingFileMessage createMessageForRetry(int sequenceNumber)
{
    ScheduledFuture future = timeoutTasks.get(sequenceNumber);
    future.cancel(false);
    return files.get(sequenceNumber);
}
```

```java
// AFTER
public synchronized OutgoingFileMessage createMessageForRetry(int sequenceNumber)
{
    ScheduledFuture future = timeoutTasks.get(sequenceNumber);
    if (future != null)
        future.cancel(false);
    return files.get(sequenceNumber);
}
```

---

## How to Spot

Any time a value from a `Map.get()` is used immediately without a null check, flag it for review — especially in retry/error-recovery paths where the expected state (a registered timeout task) may have already been cleaned up.

---

## Test

No test added.
