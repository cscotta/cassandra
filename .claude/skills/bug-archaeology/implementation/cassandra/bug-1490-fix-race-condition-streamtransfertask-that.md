# Bug 1490: Fix Race Condition in StreamTransferTask That Could Cause Missed File Sends

**Commit**: 8e42d5fe86 · **Ticket**: CASSANDRA-7704
**Subsystem**: src/java/org/apache/cassandra/streaming
**Category**: Race condition / missing synchronization

---

## Root Cause

`StreamTransferTask` used `ConcurrentHashMap` for `files` and `timeoutTasks` but relied on multi-step compound operations (check-then-act on map contents, check executor state, cancel futures) that were not atomically synchronized. Concurrently, the `complete()` and `scheduleTimeout()` methods could race: a timeout task could fire just as `complete()` was running, leading to double-release of SSTable references or `taskCompleted()` being called multiple times. Separately, checking `timeoutExecutor.isShutdown()` in `scheduleTimeout()` was not an atomic guard against a concurrent `abort()`. The fix replaces `ConcurrentHashMap` with plain `HashMap` and makes all mutating methods `synchronized` on `this`, ensuring all state transitions are serialized.

### Before / After

```java
// BEFORE
private final Map<Integer, OutgoingFileMessage> files = new ConcurrentHashMap<>();
private final Map<Integer, ScheduledFuture> timeoutTasks = new ConcurrentHashMap<>();

public synchronized void complete(int sequenceNumber)
{
    OutgoingFileMessage file = files.remove(sequenceNumber);
    if (file != null)
    {
        file.sstable.releaseReference();
        if (files.isEmpty())
        {
            timeoutExecutor.shutdownNow(); // races with scheduleTimeout
            session.taskCompleted(this);
        }
    }
}
```

```java
// AFTER
private final Map<Integer, OutgoingFileMessage> files = new HashMap<>();
private final Map<Integer, ScheduledFuture> timeoutTasks = new HashMap<>();

public void complete(int sequenceNumber)
{
    boolean signalComplete;
    synchronized (this)
    {
        ScheduledFuture timeout = timeoutTasks.remove(sequenceNumber);
        if (timeout != null)
            timeout.cancel(false);
        OutgoingFileMessage file = files.remove(sequenceNumber);
        if (file != null)
            file.sstable.releaseReference();
        signalComplete = files.isEmpty();
    }
    if (signalComplete)
        session.taskCompleted(this); // called outside lock to avoid deadlock
}
```

---

## How to Spot

When multiple related data structures (files map + timeout map) are modified together, using a concurrent collection for each independently does not guarantee atomicity across the pair. Compound operations always require explicit locking. Also watch for state checks like `executor.isShutdown()` used as guards when the shutdown itself is triggered by a competing thread.

---

## Test

`StreamTransferTaskTest` was updated to verify that completing a file before its timeout correctly cancels the timeout future (expects `CancellationException`), and that a timeout firing on the last pending file drives the session to `WAIT_COMPLETE`.
