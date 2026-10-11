# Bug 1989: Race Condition Starting exitThread Multiple Times on OOM

**Commit**: 40f0bdce06 · **Ticket**: CASSANDRA-5716
**Subsystem**: src/java/org/apache/cassandra/service/CassandraDaemon.java
**Category**: Race condition / missing synchronization

---

## Root Cause

The uncaught exception handler in `CassandraDaemon` called `exitThread.start()` whenever an `OutOfMemoryError` was detected. Under concurrent OOM conditions (e.g., multiple threads hitting OOM simultaneously), multiple threads could each reach the `exitThread.start()` call, and calling `Thread.start()` on an already-running thread throws `IllegalThreadStateException`. The fix wraps the start call in a `synchronized` block and guards it with `exitThread.isAlive()`, ensuring the thread is started at most once.

### Before / After

```java
// BEFORE: multiple OOM-handling threads can all call start() concurrently
if (e2 instanceof OutOfMemoryError)
    exitThread.start();
```

```java
// AFTER: synchronized guard prevents double-start
if (e2 instanceof OutOfMemoryError)
{
    synchronized (exitThread)
    {
        if (!exitThread.isAlive())
            exitThread.start();
    }
}
```

---

## How to Spot

Any code path that starts a singleton thread from an error handler (especially one triggered by multiple threads like OOM) must guard against concurrent invocations. Pattern: `if (!thread.isAlive()) thread.start()` without synchronization is still racy; both the check and the start must be inside the same critical section.

---

## Test

No new test added.
