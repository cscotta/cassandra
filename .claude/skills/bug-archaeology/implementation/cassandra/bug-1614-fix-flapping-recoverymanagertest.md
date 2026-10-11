# Bug 1614: Flapping RecoveryManagerTest

**Commit**: 484d281694 · **Ticket**: CASSANDRA-7084
**Subsystem**: db/commitlog/CommitLogAllocator
**Category**: Race condition / missing synchronization

---

## Root Cause

`CommitLogAllocator.teardown()` waited for its own internal segment queue to drain (`queue.isEmpty()`) before closing all commit log segments. However, it did not wait for write operations that had already been dispatched to the `StorageService.tasks` executor to complete. Those tasks were still running asynchronously and could be writing to commit log segments even as `teardown()` closed them, causing test failures due to I/O on closed files and inconsistent state during recovery replay.

The fix adds two things: first, it spins until `StorageService.tasks.getActiveCount() == 0` (all in-flight write tasks have finished), and second, it adds a 10 ms sleep after the queue drains to let any in-flight task complete its final writes before segments are closed.

### Before / After

```java
// BEFORE — only waits for queue to drain, not for active tasks
while (!queue.isEmpty())
    Thread.yield();

for (CommitLogSegment segment : Iterables.concat(activeSegments, availableSegments))
    segment.close();
```

```java
// AFTER — waits for both active executor tasks and empty queue, plus brief grace period
while (StorageService.tasks.getActiveCount() > 0 || !queue.isEmpty())
    Thread.yield();

Uninterruptibles.sleepUninterruptibly(10, TimeUnit.MILLISECONDS);

for (CommitLogSegment segment : Iterables.concat(activeSegments, availableSegments))
    segment.close();
```

---

## How to Spot

When a teardown/shutdown sequence needs to close shared resources, it must wait on all producers of those resources — not just the queue. If work is dispatched to an external thread pool and then placed onto a queue, draining the queue alone is insufficient; the executor's active task count must also reach zero first.

---

## Test

`RecoveryManagerTest` was annotated with `@RunWith(OrderedJUnit4ClassRunner.class)` to ensure deterministic test ordering. The source fix (waiting for active tasks) is what actually prevents the flap; the test change ensures test isolation.
