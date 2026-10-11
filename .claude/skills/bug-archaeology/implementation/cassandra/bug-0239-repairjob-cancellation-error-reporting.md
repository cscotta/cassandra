# Bug 0239: RepairJob Spuriously Reports Cancellation as an Error

**Commit**: f6a04ca81f · **Ticket**: CASSANDRA-17701
**Subsystem**: src/java/org/apache/cassandra/repair/RepairJob.java
**Category**: Logic error in condition

---

## Root Cause

`RepairJob.run()` used `Futures.getUnchecked(validations)` to wait for the validation phase. When the job was cancelled, `getUnchecked` re-threw the `CancellationException` as an `UncheckedExecutionException`, which bubbled up through the repair pipeline as an unexpected error even though cancellation is a normal, handled outcome. The result was misleading error logging and incorrect failure reporting for repair jobs that were intentionally cancelled.

### Before / After

```java
// BEFORE — getUnchecked wraps CancellationException as an unexpected error
Futures.getUnchecked(validations);
```

```java
// AFTER — explicit catch suppresses expected cancellation/execution exceptions
try
{
    syncResults.get();
}
catch (CancellationException | ExecutionException ignored)
{
    // this is already handled in the listener
}
catch (InterruptedException e)
{
    Thread.currentThread().interrupt();
    throw new UncheckedExecutionException(e);
}
```

---

## How to Spot

`Futures.getUnchecked()` is a convenience wrapper that converts both `CancellationException` and `ExecutionException` into `UncheckedExecutionException`, making them indistinguishable from true failures. Wherever a future may be intentionally cancelled, prefer explicit `Future.get()` with a typed catch so that normal lifecycle events are not confused with errors. The fix also preserves the interrupted-thread contract by re-interrupting the current thread.

---

## Test

No test added; the fix was validated by running a repair and cancelling it mid-way, then confirming no spurious error is recorded in the repair status or logs.
