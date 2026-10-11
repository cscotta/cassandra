# Bug 2061: Exceptions in ScheduledThreadPoolExecutor tasks silently swallowed

**Commit**: 4deb77971a · **Ticket**: CASSANDRA-2061

## Detection Rule
When overriding `afterExecute` to log exceptions, check `(r instanceof Future<?>) && ((Future<?>)r).isDone()` rather than `r instanceof FutureTask<?>`, because `ScheduledFutureTask` wraps `FutureTask` and calling `get()` on a non-done scheduled task blocks indefinitely.

## Code Smell
`if (r instanceof FutureTask<?>) { ((FutureTask<?>)r).get(); }` in `afterExecute` will block if the task is a repeating `ScheduledFutureTask` that is not yet done, hiding the exception and freezing the thread.

## Tags
- scope: cassandra
- category: silent-skip
- code-pattern: futuretask-get-blocks-on-scheduled
- severity: silent-wrong-result
- subsystem: network-messaging

## Root Cause
`DebuggableThreadPoolExecutor.logExceptionsAfterExecute` used `instanceof FutureTask<?>` to detect wrapped exceptions. When tasks were submitted to a `ScheduledThreadPoolExecutor`, the runnable was wrapped in a `ScheduledFutureTask` (which is `Future<?>` but not `FutureTask<?>`), so exceptions were never extracted. Additionally, calling `get()` on a non-done `ScheduledFutureTask` would block the executor thread. The fix changes the check to `(r instanceof Future<?>) && ((Future<?>)r).isDone()`.

## Example
```java
// BEFORE
if (r instanceof FutureTask<?>) {
    try { ((FutureTask<?>) r).get(); }
    catch (...) { ... }
}
```
```java
// AFTER
if ((r instanceof Future<?>) && ((Future<?>) r).isDone()) {
    try { ((Future<?>) r).get(); }
    catch (...) { ... }
}
```
