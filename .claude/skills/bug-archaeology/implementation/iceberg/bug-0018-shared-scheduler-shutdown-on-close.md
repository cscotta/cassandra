# Bug 0018: Shared scheduler shut down by individual lock manager close, causing RejectedExecutionException for concurrent catalogs

**Commit**: 1b733ed22c8f47965184a48017c5941014def4cf · **Ticket**: #15862

## Detection Rule

When a class holds a reference to a static or otherwise shared executor/scheduler, verify that its `close()` method does not call `shutdown()` or `shutdownNow()` on that shared resource — only the true owner (typically the JVM shutdown hook or a dedicated lifecycle manager) should terminate it.

## Code Smell

A `close()` method that calls `scheduler.shutdownNow()` (or `executor.shutdown()`) on a field that was initialized in a `synchronized` block guarded by a class-level lock (e.g. `synchronized (BaseLockManager.class)`) — the class-level guard signals the scheduler is shared across instances, but `close()` tears it down anyway.

## Tags

- scope: java
- category: state-not-cleaned
- code-pattern: shared-resource-shutdown-on-close
- severity: crash
- subsystem: core / lock-manager

## Root Cause

`BaseLockManager` lazily initializes a single static `ScheduledExecutorService` shared by all instances of the class. When any one instance was closed (e.g. when a catalog using the default lock manager was stopped), `close()` called `scheduler.shutdownNow()` and set `scheduler = null`. Any subsequent lock operation on a different catalog instance that still held a reference to the now-terminated scheduler threw `RejectedExecutionException`. The fix removes the shutdown call from `close()`; the scheduler uses daemon threads and is terminated only at JVM exit via the registered `MoreExecutors.getExitingScheduledExecutorService` shutdown hook.

## Example

```java
// BEFORE — shuts down the shared static scheduler on every close()
@Override
public void close() throws Exception {
    if (scheduler != null) {
        List<Runnable> tasks = scheduler.shutdownNow();
        tasks.forEach(task -> {
            if (task instanceof Future) {
                ((Future<?>) task).cancel(true);
            }
        });
        scheduler = null;
    }
}
```

```java
// AFTER — individual instances must not shut down the shared scheduler
@Override
public void close() throws Exception {
    // The scheduler is a shared static resource used across all BaseLockManager instances.
    // Individual instances must not shut it down, as other instances may still be using it.
    // The scheduler uses daemon threads and will be terminated at JVM exit by the shutdown
    // hook registered via MoreExecutors.getExitingScheduledExecutorService.
}
```
