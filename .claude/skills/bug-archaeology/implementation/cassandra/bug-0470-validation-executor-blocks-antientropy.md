# Bug 0470: ValidationExecutor Blocks AntiEntropyStage When Saturated

**Commit**: 04533e6cda · **Ticket**: CASSANDRA-15812
**Subsystem**: `src/java/org/apache/cassandra/db/compaction/CompactionManager.java`, `src/java/org/apache/cassandra/service/ActiveRepairService.java`
**Category**: Deadlock

---

## Root Cause

`ValidationExecutor` was constructed with a `SynchronousQueue` as its work queue and a `corePoolSize` of 1. `SynchronousQueue` has zero capacity: a call to `submit()` blocks the calling thread until a worker thread picks up the task. The submitting thread is `AntiEntropyStage`, the single thread responsible for coordinating all repair activity. When `ValidationExecutor` was fully saturated, every subsequent repair submission would block `AntiEntropyStage` indefinitely, preventing all repair progress and causing session timeouts. The fix switches to `LinkedBlockingQueue` (unbounded capacity), sets `corePoolSize` equal to `concurrent_validations` so the executor queues rather than spawns new threads, and adds `allowCoreThreadTimeOut(true)` so idle threads are eventually reclaimed.

### Before / After

```java
// BEFORE
public ValidationExecutor() {
    super(1, DatabaseDescriptor.getConcurrentValidations(),
          "ValidationExecutor", new SynchronousQueue<Runnable>());
}
```

```java
// AFTER
public ValidationExecutor() {
    super(DatabaseDescriptor.getConcurrentValidations(),
          DatabaseDescriptor.getConcurrentValidations(),
          "ValidationExecutor",
          new LinkedBlockingQueue());
    allowCoreThreadTimeOut(true);
}
```

---

## How to Spot

`SynchronousQueue` is appropriate only when the submitter must never block — it requires an immediately-available worker. Using it as a thread-pool work queue with `ThreadPoolExecutor.CallerRunsPolicy` or a blocking handler means the submitting thread stalls whenever the pool is saturated. For executors fed by a critical coordination thread, always use a bounded or unbounded `BlockingQueue` that allows tasks to be queued without blocking the submitter.

---

## Test

`ValidationExecutorTest.testQueueOnValidationSubmission` submits 5 tasks to a 2-thread executor and asserts that 2 are active and 3 are queued rather than blocking; `testAdjustPoolSize` verifies that `adjustPoolSize()` dynamically resizes both core and max pool sizes.
