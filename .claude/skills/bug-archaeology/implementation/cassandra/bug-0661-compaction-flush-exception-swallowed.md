# Bug 0661: Compaction and Flush Exceptions Silently Swallowed by submit()

**Commit**: e80ede6d39 · **Ticket**: CASSANDRA-13833
**Subsystem**: src/java/org/apache/cassandra/db/ColumnFamilyStore.java, src/java/org/apache/cassandra/db/compaction/CompactionManager.java
**Category**: Resource leak

---

## Root Cause

Both `ColumnFamilyStore` and `CompactionManager.CompactionExecutor` submitted `ListenableFutureTask` instances via `ExecutorService.submit(Runnable)` instead of `execute(Runnable)`. `submit()` wraps the task in an internal `FutureTask` managed by the executor, effectively replacing the `ListenableFutureTask` wrapper; this meant that any exception thrown by the task was captured inside the executor's own `Future` rather than propagated through the `ListenableFutureTask`'s listeners. Callers that attached Guava listeners to the returned `ListenableFutureTask` to observe failure were never notified, so compaction and flush errors were silently dropped.

### Before / After

```java
// BEFORE (ColumnFamilyStore)
flushExecutor.submit(flushTask);
postFlushExecutor.submit(task);

// BEFORE (CompactionExecutor)
ListenableFutureTask ret = ListenableFutureTask.create(task);
submit(ret);
return ret;
```

```java
// AFTER (ColumnFamilyStore)
flushExecutor.execute(flushTask);
postFlushExecutor.execute(task);

// AFTER (CompactionExecutor)
ListenableFutureTask ret = ListenableFutureTask.create(task);
execute(ret);
return ret;
```

---

## How to Spot

Whenever a `ListenableFutureTask` (or any `Runnable` that carries its own result/exception container) is handed to an executor, using `submit()` will re-wrap it and shadow the internal future, breaking any attached listeners. The safe pattern is `execute()`, which runs the `Runnable` directly. Reviewers should treat any `executor.submit(futureTask)` call as a bug candidate and verify whether the returned `Future` from `submit()` is the same object as the input.

---

## Test

The added `CompactionExecutorTest` verifies that exceptions thrown inside tasks submitted to `CompactionExecutor` are observable through the returned `ListenableFutureTask`, and that the executor does not silently swallow them.
