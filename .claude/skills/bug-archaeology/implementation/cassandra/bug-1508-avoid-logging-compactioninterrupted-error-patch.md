# Bug 1508: Avoid logging CompactionInterrupted at ERROR

**Commit**: 6e6a6c3979 · **Ticket**: CASSANDRA-7694
**Subsystem**: src/java/org/apache/cassandra/db/compaction/CompactionManager.java
**Category**: Wrong constant / default value

---

## Root Cause

`CompactionManager` uses an inner `DebuggableThreadPoolExecutor` subclass that overrides `afterExecute`. The override called `super.afterExecute(r, t)` first — which invoked `logExceptionsAfterExecute` — and then extracted the throwable from the completed future. When compaction was intentionally interrupted (`CompactionInterruptedException`), the exception was still present in `t` at the point `logExceptionsAfterExecute` ran, so it was logged at ERROR level even though a compaction interruption is a normal, expected event.

The fix separates `maybeResetTraceSessionWrapper` into a standalone static helper. The compaction executor's `afterExecute` now calls only that helper (to clean up any trace context) before doing its own exception extraction and classification, bypassing the ERROR-level log path for interruptions.

### Before / After

```java
// BEFORE — super.afterExecute runs logExceptionsAfterExecute before the throwable is inspected
@Override
public void afterExecute(Runnable r, Throwable t)
{
    super.afterExecute(r, t);  // logs at ERROR unconditionally
    if (t == null)
        t = DebuggableThreadPoolExecutor.extractThrowable(r);
    ...
}
```

```java
// AFTER — only reset trace state; skip the blanket error logger
@Override
public void afterExecute(Runnable r, Throwable t)
{
    DebuggableThreadPoolExecutor.maybeResetTraceSessionWrapper(r);
    if (t == null)
        t = DebuggableThreadPoolExecutor.extractThrowable(r);
    ...
}
```

---

## How to Spot

Calling `super.afterExecute` in a thread-pool subclass inherits all of the parent's exception-logging behavior. When the subclass needs custom severity handling for specific exception types, it should not delegate to the parent's error logger. Look for `super.afterExecute` calls in executor subclasses that handle known benign exceptions downstream — the parent log call will fire before those checks are reached.

---

## Test

No test added.
