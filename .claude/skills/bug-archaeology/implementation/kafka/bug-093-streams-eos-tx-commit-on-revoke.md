**Commit**: 2181ddbb03 · **Ticket**: KAFKA-18943
**Subsystem**: streams/src/main/java/org/apache/kafka/streams/processor/internals/TaskManager.java
**Category**: Logic error in condition

**Root cause**: `handleRevocation` unconditionally called `prepareCommitAndAddOffsetsToMap(revokedActiveTasks, ...)` and then committed the transaction if the resulting map was non-empty. A task that had been *revoked without ever processing any records* (idle task) populates the map with an empty offset entry, which still triggered a TX commit. Under EOS-v2, committing a transaction that contains data written by a *different* active task (one with real progress) prematurely makes those records visible to consumers before a subsequent rebalance was complete. The fix adds a `revokedTasksNeedCommit` flag, set only when a revoked task has `commitNeeded() == true`, and gates all commit operations on that flag.

**Before/After**

```java
// Before – always prepared and (conditionally) committed
prepareCommitAndAddOffsetsToMap(revokedActiveTasks, consumedOffsetsPerTask);
final boolean shouldCommitAdditionalTasks = !consumedOffsetsPerTask.isEmpty();
if (shouldCommitAdditionalTasks) { ... taskExecutor.commitOffsetsOrTransaction(...); }

// After – only when revoked task actually needs commit
boolean revokedTasksNeedCommit = false;
// ...
revokedTasksNeedCommit |= task.commitNeeded();
// ...
if (revokedTasksNeedCommit) {
    prepareCommitAndAddOffsetsToMap(revokedActiveTasks, consumedOffsetsPerTask);
    taskExecutor.commitOffsetsOrTransaction(consumedOffsetsPerTask);
}
```

**How to spot**: In EOS commit paths, "non-empty offset map" is an unreliable proxy for "needs commit" because tasks with no progress still produce entries. Use an explicit `commitNeeded()` predicate to guard transaction commits.

**Test**: `shouldNotCommitActiveTasksWithPendingInputIfRevokedTaskDidNotMakeProgress` — writes one record, adds a second stream thread to force rebalance (causing the idle task to be revoked), verifies no committed records appear in the output topic before the producer is fenced.
