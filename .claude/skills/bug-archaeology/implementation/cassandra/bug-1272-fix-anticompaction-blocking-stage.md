# Bug 1272: Anticompaction Blocks ANTI_ENTROPY Stage

**Commit**: 6d06f32a77 · **Ticket**: CASSANDRA-9151
**Subsystem**: service/ActiveRepairService.java, db/compaction/CompactionManager.java
**Category**: Deadlock

---

## Root Cause

Anti-compaction was submitted to the compaction executor via `executor.submit(runnable)`, which returns a plain `Future` whose completion is not linked to the submitted task—the future would immediately resolve without waiting for the runnable to actually finish. The verb handler for `ANTICOMPACTION_REQUEST` then called `FBUtilities.waitOnFutures(futures)` on those useless futures, believing it was waiting for compaction to complete, but it was actually blocking the `ANTI_ENTROPY` messaging thread on nothing useful while simultaneously holding the handler thread and preventing further repair coordination messages from being processed. The structural failure is that the `Future` returned by a raw `ExecutorService.submit(runnable)` reflects only queueing, not execution completion, when the submitted task is already a `Runnable` wrapper.

### Before / After

```java
// BEFORE
public Future<?> submitAntiCompaction(...) {
    ...
    return executor.submit(runnable);  // returns opaque future; completion not linked to task
}

// In verb handler:
List<Future<?>> futures = ActiveRepairService.instance.doAntiCompaction(parentRepairSession);
FBUtilities.waitOnFutures(futures);  // blocks ANTI_ENTROPY thread
```

```java
// AFTER
public ListenableFuture<?> submitAntiCompaction(...) {
    ...
    ListenableFutureTask<?> task = ListenableFutureTask.create(runnable, null);
    executor.submit(task);
    return task;  // task itself is the future; completion IS linked
}

// doAntiCompaction now registers a listener to remove the session asynchronously;
// verb handler no longer blocks:
ActiveRepairService.instance.doAntiCompaction(anticompactionRequest.parentRepairSession);
```

---

## How to Spot

Whenever a thread pool's `submit(runnable)` return value is used as a completion barrier by blocking on `.get()` or `FBUtilities.waitOnFutures()`, verify that the future truly tracks execution end and not just queue insertion. The risk is highest in message-handling stages that submit work to secondary executors: if the handler thread blocks waiting on the compaction thread, you get a cross-stage dependency that can stall repair coordination for all peers. Look for places where a plain `Runnable` is wrapped and submitted but the wrapping future's lifecycle is never properly tied to the task itself.

---

## Test

No test added.
