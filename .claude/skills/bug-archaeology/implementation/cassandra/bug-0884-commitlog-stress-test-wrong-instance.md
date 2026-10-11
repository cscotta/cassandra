# Bug 0884: CommitLogStressTest uses stale singleton and wrong segment-manager reference

**Commit**: a13add64fe · **Ticket**: CASSANDRA-12082
**Subsystem**: Commit Log
**Category**: State not cleaned up

---

## Root Cause
`CommitLogStressTest` created a fresh `CommitLog` instance per test run but then called methods on `CommitLog.instance` (the singleton), which remained from a previous or default initialization. This meant segment-manager operations such as `awaitManagementTasksCompletion()`, `enableReserveSegmentCreation()`, and `getActiveSegments()` operated on the wrong instance, producing race conditions and incorrect test results. Additionally, the `processingTask` volatile flag used in `awaitManagementTasksCompletion()` was unreliable as a completion signal: it was cleared before the task actually finished. The fix replaces the flag with a small fixed sleep after the queue drains, removes all `CommitLog.instance` references from the test in favour of the local `commitLog`, and renames the mis-spelled `getCommitLogMaxCompressionBuffersPerPool()` to `getCommitLogMaxCompressionBuffersInPool()` to match the config key.

### Before / After
```java
// BEFORE — operates on the global singleton, not the test's instance
CommitLog.instance.segmentManager.enableReserveSegmentCreation();
CommitLog.instance.segmentManager.awaitManagementTasksCompletion();
Collection<CommitLogSegment> segments = CommitLog.instance.segmentManager.getActiveSegments();

// awaitManagementTasksCompletion — unreliable busy-loop on processingTask flag
while (segmentManagementTasks.size() > 0 || processingTask)
    Thread.yield();

// AFTER — uses the test-local commitLog instance
commitLog.segmentManager.enableReserveSegmentCreation();
commitLog.segmentManager.awaitManagementTasksCompletion();
Collection<CommitLogSegment> segments = commitLog.segmentManager.getActiveSegments();

// awaitManagementTasksCompletion — drains queue then sleeps briefly for last task
while (!segmentManagementTasks.isEmpty())
    Thread.yield();
Uninterruptibles.sleepUninterruptibly(100, TimeUnit.MILLISECONDS);
```

---

## How to Spot
Whenever a test creates a new instance of a class that also has a static singleton (e.g. `CommitLog`, `StorageService`), verify that every method call in the test targets the local instance rather than the singleton. Static references to `Foo.instance` in tests are a red flag that could cause silent cross-test contamination or flaky assertions.

---

## Test
The existing `CommitLogStressTest` is the test being fixed; no additional test coverage was added beyond the bug fix itself.
