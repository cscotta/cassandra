# Bug 0964: Leak Detector Strong Reference Loop Defeats GC

**Commit**: 15092e6344 · **Ticket**: CASSANDRA-11120
**Subsystem**: SSTable I/O / reference counting
**Category**: Resource leak

---

## Root Cause
`SSTableReader.GlobalTidy` held a strong reference to the `ScheduledFuture` returned by `syncExecutor.scheduleAtFixedRate(...)`, which ran a `Runnable` that in turn referred back to the enclosing `GlobalTidy` instance (via its `desc` and `readMeter` fields). This created a strong reference cycle: `GlobalTidy → ScheduledFuture → Runnable → GlobalTidy`. Because the leak detector in `Ref` walks all reachable strong references to find cycles, it would falsely flag this as a leak and log spurious error messages. The fix wraps the `ScheduledFuture` in a `WeakReference` inside `GlobalTidy`, breaking the strong cycle; the `Ref` traversal was also updated to skip the referent fields of `WeakReference` objects (which belong to `java.lang.ref.Reference`) while still traversing any strong fields in `WeakReference` subclasses.

### Before / After
```java
// BEFORE
private ScheduledFuture readMeterSyncFuture;
...
readMeterSyncFuture = syncExecutor.scheduleAtFixedRate(new Runnable() { ... }, 1, 5, TimeUnit.MINUTES);
...
if (readMeterSyncFuture != null)
{
    readMeterSyncFuture.cancel(true);
    readMeterSyncFuture = null;
}

// AFTER
static WeakReference<ScheduledFuture<?>> NULL = new WeakReference<>(null);
private WeakReference<ScheduledFuture<?>> readMeterSyncFuture = NULL;
...
readMeterSyncFuture = new WeakReference<>(syncExecutor.scheduleAtFixedRate(new Runnable() { ... }, 1, 5, TimeUnit.MINUTES));
...
ScheduledFuture<?> readMeterSyncFutureLocal = readMeterSyncFuture.get();
if (readMeterSyncFutureLocal != null)
{
    readMeterSyncFutureLocal.cancel(true);
    readMeterSyncFuture = NULL;
}
```

---

## How to Spot
Any inner class or lambda passed to a scheduled executor that is also stored back as a field on the enclosing object creates a reference cycle. During review, check whether `ScheduledFuture` (or any `Future`) fields are held strongly by objects that the submitted `Runnable`/`Callable` also references; if so, either the future should be held weakly or the runnable should not close over the owning object. The `Ref`-based leak detector is particularly sensitive to such cycles, so spurious leak reports are a signal to look for strong-cycle closure patterns.

---

## Test
`RefCountedTest` was extended to verify that the leak detector does not report false positives when a `WeakReference`-wrapped future is present in the object graph.
