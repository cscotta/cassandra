# Bug 0590: Race condition in ScanTaskIterable causes hang or missed tasks

**Commit**: b23f13f71e7aa39b728ba0af5aa7e157f20e498b · **Ticket**: #14824

## Detection Rule

When a producer-consumer iterator uses a shared queue and multiple worker threads, verify that termination is signaled via a sentinel value (poison pill) placed into the queue by the last worker, not by polling external state flags after the queue appears empty, to avoid a TOCTOU race between "queue empty" and "workers done" checks.

## Code Smell

A `isDone()` helper that checks `taskQueue.isEmpty() && planTasks.isEmpty() && activeWorkers.get() == 0` is called from the consumer thread; because these three conditions are checked non-atomically, a window exists where the queue is empty but a worker has not yet decremented `activeWorkers`, causing the consumer to terminate early or spin indefinitely.

## Tags

- scope: distributed-systems
- category: race-condition
- code-pattern: non-atomic-write
- severity: silent-wrong-result
- subsystem: rest-catalog / scan-planning

## Root Cause

The original `isDone()` checked three independent atomics non-atomically: a worker could drain the queue and be about to decrement `activeWorkers`, but the consumer observed `taskQueue.isEmpty() && activeWorkers == 1` (not zero yet) and kept polling, or alternatively saw `activeWorkers == 0` before the last task was enqueued. Additionally, worker exceptions were thrown into the thread pool and never surfaced to the consumer. The fix introduces a `DUMMY_TASK` poison pill: the last surviving worker places it on the queue, and the consumer exits only when it dequeues it. Worker failures are captured in an `AtomicReference<RuntimeException>` and rethrown by the consumer on the next `hasNext()` call.

## Example

```java
// BEFORE — non-atomic multi-field termination check; exceptions lost
private boolean isDone() {
  return taskQueue.isEmpty()
      && planTasks.isEmpty()
      && activeWorkers.get() == 0
      && initialFileScanTasks.isEmpty();
}
// consumer:
while (true) {
  if (isDone()) return false;
  nextTask = taskQueue.poll(QUEUE_POLL_TIMEOUT_MS, TimeUnit.MILLISECONDS);
  if (nextTask != null) return true;
}
```

```java
// AFTER — poison pill signals completion; exceptions propagated to consumer
private static final FileScanTask DUMMY_TASK = new BaseFileScanTask(null, null, null, null, null);
private final AtomicReference<RuntimeException> failure = new AtomicReference<>(null);

// last worker calls signalCompletion() which enqueues DUMMY_TASK
// consumer:
while (!shutdown.get()) {
  nextTask = taskQueue.poll(QUEUE_POLL_TIMEOUT_MS, TimeUnit.MILLISECONDS);
  if (nextTask == DUMMY_TASK) { shutdown.set(true); break; }
  else if (nextTask != null) { hasNext = true; break; }
}
RuntimeException workerFailure = failure.get();
if (workerFailure != null) throw workerFailure;
return hasNext;
```
