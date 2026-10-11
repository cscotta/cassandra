# Bug 0814: RejectedExecutionException and resource leaks when submitting compaction tasks after executor shutdown

**Commit**: be6e6ea662 · **Ticket**: CASSANDRA-12457
**Subsystem**: Compaction / Shutdown / StorageService
**Category**: Resource leak

---

## Root Cause
Multiple code paths in `CompactionManager` checked `executor.isShutdown()` before calling `executor.submit()`, but the check and the submit were not atomic. A `RejectedExecutionException` could still be thrown between the guard and the submit if the executor shut down concurrently. When that happened, the submitted task was silently lost or logged incorrectly, and resources such as SSTable references held by anticompaction tasks were never released. Additionally, `submitCacheWrite` had a bug where a cancelled-future path called `Futures.immediateCancelledFuture()` without `return`, so the method fell through to `executor.submit()` anyway. The fix introduces a `submitIfRunning()` helper that atomically checks, submits, and catches `RejectedExecutionException`, always returning a `ListenableFuture<?>` that is either the real task future or an already-cancelled future; callers then check `fut.isCancelled()` instead of `executor.isShutdown()`.

Separately, the drain ordering in `StorageService` was corrected: `BatchlogManager.shutdown()` and `CompactionManager.forceShutdown()` are now called before the system-keyspace flush, because compaction tidiers write to system tables (e.g. `SSTableReader.GlobalTidy`).

### Before / After
```java
// BEFORE — racy TOCTOU pattern
if (executor.isShutdown()) {
    logger.info("Executor has shut down, not submitting background task");
    return Collections.emptyList();
}
compactingCF.add(cfs);
futures.add(executor.submit(new BackgroundCompactionCandidate(cfs)));

// AFTER — single atomic helper
Future<?> fut = executor.submitIfRunning(new BackgroundCompactionCandidate(cfs), "background task");
if (!fut.isCancelled()) {
    compactingCF.add(cfs);
    futures.add(fut);
}
```

---

## How to Spot
Any `isShutdown()` guard followed by `submit()` on a different line is a TOCTOU race. The correct pattern is to attempt the submit and catch `RejectedExecutionException`. Also watch for `return`-less branches that call `Futures.immediateCancelledFuture()` — if the return value is not used, the guard has no effect. Finally, audit drain/shutdown sequences for ordering: tasks that update system state must be stopped before the system tables are flushed.

---

## Test
No new test was added; the fix is validated by the absence of the `RejectedExecutionException` and resource-leak log messages observed during draining.
