# Bug 1368: StreamTransferTask Race Condition Causes Infinite Loops and Premature SSTable Deletion

**Commit**: ddca610c9c · **Ticket**: CASSANDRA-7704
**Subsystem**: src/java/org/apache/cassandra/streaming/StreamTransferTask.java
**Category**: Race condition / missing synchronization

---

## Root Cause

`StreamTransferTask` used `ConcurrentHashMap` for `files` and `timeoutTasks`, and an `AtomicBoolean` for `aborted`, but coordinated multi-step operations across those collections without a single lock. A per-instance `ScheduledExecutorService` was shut down instead of cancelled per-file, meaning a timeout could fire after `complete()` had already removed the entry, re-enter `complete()`, and decrement the task count again—potentially signalling session completion prematurely or infinitely looping. Simultaneously, `abort()` could release SSTable references while `complete()` was also releasing the same reference, causing a double-release.

### Before / After

```java
// BEFORE
public synchronized void complete(int sequenceNumber)
{
    OutgoingFileMessage file = files.remove(sequenceNumber);
    if (file != null)
    {
        file.sstable.releaseReference();
        if (files.isEmpty())
        {
            timeoutExecutor.shutdownNow();  // shuts down executor, but racing timeout may have already fired
            session.taskCompleted(this);
        }
    }
}
```

```java
// AFTER
public void complete(int sequenceNumber)
{
    boolean signalComplete;
    synchronized (this)
    {
        ScheduledFuture timeout = timeoutTasks.remove(sequenceNumber);
        if (timeout != null)
            timeout.cancel(false);  // cancel per-file timeout explicitly

        OutgoingFileMessage file = files.remove(sequenceNumber);
        if (file != null)
            file.sstable.releaseReference();

        signalComplete = files.isEmpty();
    }
    // session callback called outside the lock to avoid deadlock
    if (signalComplete)
        session.taskCompleted(this);
}
```

---

## How to Spot

When a class uses multiple concurrent collections (here `ConcurrentHashMap`) to represent related state, but then performs multi-step operations that must be atomic across all of them, the concurrent collection types provide no inter-collection atomicity guarantee. Look for patterns where a "remove from map A, then check map B" sequence is not enclosed in a single `synchronized` block—especially when a background executor can fire callbacks that touch the same maps.

---

## Test

Test updated to verify that completing a file before its timeout fires causes the timeout future to be cancelled (throwing `CancellationException` on `get()`), and that completing the last file transitions the session to `WAIT_COMPLETE`.
