# Bug 2717: Counter write deadlock when coordinator is also a replica

**Commit**: 21015c435b · **Ticket**: CASSANDRA-2454

## Detection Rule
When the same thread that dispatches work to an executor is also the executor's worker thread, submitting new tasks from within a task causes a deadlock.

## Code Smell
`applyCounterMutation` unconditionally calls `StageManager.getStage(Stage.MUTATION).execute(runnable)`, even when it is already running on the MUTATION stage (e.g., inside `CounterMutationVerbHandler`), which blocks the stage thread waiting for itself.

## Tags
- scope: cassandra
- category: deadlock
- code-pattern: executor-self-submission
- severity: hang
- subsystem: storage-engine

## Root Cause
Counter writes have two code paths: the coordinator applies the mutation directly (via `StorageProxy`), and replicas apply it via `CounterMutationVerbHandler`, which already executes on the MUTATION stage. The single `counterWritePerformer` always dispatched work to the MUTATION stage. When a coordinator node is also a replica the replica path runs inside a MUTATION stage thread that then submits another task to the same single-threaded stage, deadlocking. The fix introduces `counterWriteOnCoordinatorPerformer` which calls `runnable.run()` inline (no re-dispatch) for the coordinator path, while the replica path keeps dispatching to the stage.

## Example
```java
// BEFORE — always dispatches to MUTATION stage (deadlocks if already on it)
private static void applyCounterMutation(...) {
    ...
    StageManager.getStage(Stage.MUTATION).execute(runnable);
}
```
```java
// AFTER — coordinator runs inline; replica dispatches to stage
private static void applyCounterMutation(..., boolean executeOnMutationStage) {
    ...
    if (executeOnMutationStage)
        StageManager.getStage(Stage.MUTATION).execute(runnable);
    else
        runnable.run();
}
```
