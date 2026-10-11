# Bug 2892: Counter replicate-on-write unnecessarily triggers a read even when no remote replicas exist

**Commit**: c9ff755776 (2612) · **Ticket**: CASSANDRA-2892

## Detection Rule
When deciding whether to replicate a counter write, check that the hinted-endpoints map is non-empty after removing the local address before submitting the expensive read-then-replicate path.

## Code Smell
`if (cm.shouldReplicateOnWrite())` without `&& !hintedEndpoints.isEmpty()` submits a replication task that performs a read even when all hinted endpoints are local (i.e., `CL=ONE` with a single-node DC), burning I/O unnecessarily.

## Tags
- scope: cassandra
- category: logic-error
- code-pattern: unnecessary-read-missing-nonempty-guard
- severity: performance
- subsystem: storage-engine

## Root Cause
`StorageProxy.counterWriteTask()` checked `cm.shouldReplicateOnWrite()` and always submitted a stage that performs `makeReplicationMutation()` (which requires a read). When the local node was the only replica (e.g. CL=ONE), the `hintedEndpoints` map would be empty after the local address was removed, making the replication completely no-op yet still paying the read cost. The fix removes the local address from `hintedEndpoints` before the check and adds `&& !hintedEndpoints.isEmpty()` as a precondition.

## Example
```java
// BEFORE
if (cm.shouldReplicateOnWrite()) {
    StageManager.getStage(Stage.READ).execute(new DroppableRunnable(...) {
        public void runMayThrow() throws IOException {
            sendToHintedEndpoints(cm.makeReplicationMutation(), hintedEndpoints, ...);
        }
    });
}
```
```java
// AFTER
hintedEndpoints.removeAll(FBUtilities.getLocalAddress());
if (cm.shouldReplicateOnWrite() && !hintedEndpoints.isEmpty()) {
    StageManager.getStage(Stage.READ).execute(new DroppableRunnable(...) {
        public void runMayThrow() throws IOException {
            sendToHintedEndpoints(cm.makeReplicationMutation(), hintedEndpoints, ...);
        }
    });
}
```
