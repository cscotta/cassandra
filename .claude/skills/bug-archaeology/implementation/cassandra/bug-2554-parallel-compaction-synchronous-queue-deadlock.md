# Bug 2554: ParallelCompactionIterable deadlocks on full SynchronousQueue

**Commit**: 7954d69001 · **Ticket**: CASSANDRA-3270

## Detection Rule
When a `ThreadPoolExecutor` uses a `SynchronousQueue` and its `RejectedExecutionHandler` blocks the caller, and the executor is created with a plain `ThreadPoolExecutor` rather than the subclass that installs that handler, task submission deadlocks because the default handler throws instead of blocking.

## Code Smell
A `ThreadPoolExecutor` is constructed with `new SynchronousQueue<Runnable>()` and a separate `setRejectedExecutionHandler(DebuggableThreadPoolExecutor.blockingExecutionHandler)` call. If `setRejectedExecutionHandler` is never reached (e.g., field is initialized inline before the constructor body runs, or on a different code path), the default `AbortPolicy` throws `RejectedExecutionException` instead of blocking.

## Tags
- scope: cassandra
- category: logic-error
- code-pattern: missing-blocking-rejection-handler
- severity: crash
- subsystem: compaction

## Root Cause
`ParallelCompactionIterable.Reducer` constructed a raw `ThreadPoolExecutor` and then called `setRejectedExecutionHandler` in the constructor body. If the queue was full when `reduce()` submitted a task, the default abort policy fired before the handler was ever set, throwing `RejectedExecutionException`. Replacing the construction with `DebuggableThreadPoolExecutor` (which installs the blocking handler automatically) eliminates the race.

## Example
```java
// BEFORE
executor = new ThreadPoolExecutor(nCPU, nCPU, ..., new SynchronousQueue<>(), ...);
executor.setRejectedExecutionHandler(
    DebuggableThreadPoolExecutor.blockingExecutionHandler);
```
```java
// AFTER
executor = new DebuggableThreadPoolExecutor(nCPU, ...,
    new SynchronousQueue<>(),
    new NamedThreadFactory("CompactionReducer"));
// blocking handler installed internally
```
