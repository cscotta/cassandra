# Bug 0419: Unbounded Number of Pending Memtable Flush Tasks Under Memory Pressure

**Commit**: 0a1e900a0a · **Ticket**: CASSANDRA-16261
**Subsystem**: utils/memory (MemtableCleanerThread / MemtablePool)
**Category**: Race condition / missing synchronization

---

## Root Cause

`MemtableCleanerThread` fired its `cleaner.run()` (which queued a flush) each time through its tight `while(needsCleaning())` loop without waiting for the previously submitted flush to complete.  Because `FlushLargestColumnFamily.run()` returned immediately after calling `switchMemtableIfCurrent()` (fire-and-forget), the cleaner could spin and schedule hundreds of flush tasks before a single one finished, exhausting executor queue memory and causing threads to pile up.  The fix converted the cleaner interface to `MemtableCleaner` returning a `CompletableFuture<Boolean>`, tracked the number of in-flight tasks via an `AtomicInteger`, and only re-signalled the wait queue after a flush completed and `needsCleaning()` was still true.

### Before / After

```java
// BEFORE – fire-and-forget loop: new flush kicked off every iteration
public void run() throws InterruptedException
{
    if (needsCleaning())
        cleaner.run();  // returns immediately; loop spins again immediately
    else { /* wait */ }
}
```

```java
// AFTER – async, back-pressured: increments counter before flush, decrements in completion handler
int numPendingTasks = this.numPendingTasks.incrementAndGet();
cleaner.clean().handle(this::apply);  // apply() decrements counter and re-signals only if still needed
```

---

## How to Spot

Whenever a background thread is supposed to limit concurrency of an async operation, verify that it actually waits for — or counts — the in-flight work before re-enqueueing.  A `Runnable` cleaner interface with a `void run()` return type cannot convey completion back to the scheduler; any scheduler that calls such a runnable in a loop without a synchronization point will over-schedule.

---

## Test

New `MemtableCleanerThreadTest` verifies that `numPendingTasks()` never exceeds the expected bound when the pool is under pressure, and that tasks drain fully after pressure is released.
