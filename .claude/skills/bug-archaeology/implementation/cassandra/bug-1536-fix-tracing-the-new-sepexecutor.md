# Bug 1536: Fix Tracing on the New SEPExecutor

**Commit**: d4d6246846 · **Ticket**: CASSANDRA-7644
**Subsystem**: src/java/org/apache/cassandra/concurrent
**Category**: State not cleaned up

---

## Root Cause

`AbstractTracingAwareExecutorService` submitted tasks without capturing the current `TraceState` at submission time, passing `null` instead of `Tracing.instance.get()`. This caused two problems: (1) tasks submitted while tracing was active silently dropped the trace context, so their work was not recorded; and (2) `TraceSessionFutureTask.run()` always reset the trace state to `null` in its `finally` block, clobbering any pre-existing state on the thread rather than restoring it.

### Before / After

```java
// BEFORE
protected <T> FutureTask<T> newTaskFor(Runnable runnable, T result)
{
    return newTaskFor(runnable, result, null);  // loses current trace state
}

// in run():
finally
{
    Tracing.instance.set(null);  // clobbers any pre-existing state
}
```

```java
// AFTER
protected <T> FutureTask<T> newTaskFor(Runnable runnable, T result)
{
    return newTaskFor(runnable, result, Tracing.instance.get());  // captures state at submit time
}

// in run():
TraceState oldState = Tracing.instance.get();
Tracing.instance.set(state);
try { ... }
finally
{
    Tracing.instance.set(oldState);  // restores previous state
}
```

---

## How to Spot

When an executor service wraps tasks with thread-local state, the capture must happen at *submit* time on the submitting thread, not lazily inside the task. Also watch for `finally` blocks that unconditionally null out thread-local state — they should restore the previous value instead.

---

## Test

No test added.
