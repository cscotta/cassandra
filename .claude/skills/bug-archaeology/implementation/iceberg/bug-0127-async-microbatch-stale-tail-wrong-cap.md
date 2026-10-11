# Bug 0127: Async microbatch planner uses stale tail field and wrong snapshot cap comparison

**Commit**: 850480018ed371bfbc7c28a68fb0a21a21a23f6b · **Ticket**: #15670

## Detection Rule

When a concurrent queue's "tail" element is tracked as a separate volatile field that is updated inside a `synchronized` block, verify that the field is always read within the same critical section; a field read outside the lock sees a stale value that may not match the current queue state.

## Code Smell

A `volatile` field (e.g. `tail`) that is written inside `synchronized(queue)` but read outside any lock in a separate method, combined with snapshot-ID boundary logic using `>=` instead of `==` for identity comparison, both indicating that the shared state is never fully consistent.

## Tags

- scope: distributed-systems
- category: race-condition
- code-pattern: stale-state-after-lifecycle
- severity: silent-wrong-result
- subsystem: spark / v4.1 / AsyncSparkMicroBatchPlanner

## Root Cause

Three distinct bugs were bundled in this fix. First, `tail` was a `volatile` field written inside `synchronized(queue)` but read in `latestOffset()` outside any lock, creating a TOCTOU race where `tail` could refer to a removed element or be `null` while the queue still contained entries. Second, the `Trigger.AvailableNow` cap check used `snapshotId() >=` instead of `snapshotId() ==`, which incorrectly stopped preloading when a snapshot whose ID happened to be numerically less than the cap ID was encountered. Third, `fillQueueFailedThrowable` was never checked in the `planFiles` termination loop, so queue-fill failures were silently swallowed. The fix replaces `tail` with a snapshot of the queue taken atomically, switches `LinkedBlockingQueue` to `LinkedBlockingDeque` (enabling `pollFirst`/`peekFirst`/`addLast` semantics), and adds the missing error propagation.

## Example

```java
// BEFORE — stale tail field and wrong cap comparison
private volatile Pair<StreamingOffset, FileScanTask> tail;

synchronized (queue) {
  queue.add(elem);
  tail = elem;
}

// cap check
if (readFrom.snapshotId() >= lastOffsetForTriggerAvailableNow.snapshotId()) { ... }

// latestOffset reads tail outside lock
if (tail != null) {
  StreamingOffset tailOffset = tail.first();
  ...
}
```

```java
// AFTER — snapshot the deque, use == for identity, propagate fill errors
queue.addLast(elem);

// cap check
if (readFrom.snapshotId() == lastOffsetForTriggerAvailableNow.snapshotId()) { ... }

// latestOffset snapshots the deque atomically
List<Pair<StreamingOffset, FileScanTask>> queueSnapshot = Lists.newArrayList(queue);
Pair<StreamingOffset, FileScanTask> queueTail =
    queueSnapshot.isEmpty() ? null : queueSnapshot.get(queueSnapshot.size() - 1);
```
