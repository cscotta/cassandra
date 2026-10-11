# Bug 0167: Multi-Table / Multi-VNode Repair Exceeds repair_session_space Causing OOM

**Commit**: 505f5af645 · **Ticket**: CASSANDRA-19336
**Subsystem**: src/java/org/apache/cassandra/repair
**Category**: Wrong constant / default value

---

## Root Cause

`repair_session_space_in_mb` was intended to cap the total heap used by Merkle tree calculations during a single repair session. However, when a single `nodetool repair` command covers multiple tables or multiple virtual-node ranges, it spawns one `RepairJob` per (range, table) combination and all validation tasks ran concurrently without any per-command cap on in-flight Merkle trees. Each individual tree obeyed the per-session limit, but the aggregate across all concurrently running jobs could far exceed it, leading to out-of-memory errors. The fix introduces a `Scheduler` abstraction and a new `concurrent_merkle_tree_requests` config property (default 0 = unlimited for backward compatibility). The scheduler queues validation tasks so that no more than the configured number of Merkle trees exist in memory simultaneously across all jobs within a single repair command.

### Before / After

```java
// BEFORE – validations submitted directly to executor without global concurrency cap
ListenableFuture<List<TreeResponse>> validations;
// ... snapshot tasks then directly transform to validations
ListenableFuture<List<SyncStat>> syncResults = Futures.transformAsync(validations, ...);
```

```java
// AFTER – validations and sync task creation wrapped in a scheduler-controlled task
ListenableFuture<List<SyncStat>> syncResults = Futures.transformAsync(
    session.validationScheduler.schedule(() -> createSyncTasks(allSnapshotTasks, allEndpoints), taskExecutor),
    this::executeTasks, taskExecutor);
```

---

## How to Spot

Any system that has a per-unit resource limit (memory, connections, file handles) but launches multiple units concurrently without a global concurrency cap can silently violate that limit by a factor equal to the concurrency. During review, whenever a bound is documented as per-task or per-session, look for the call site that spawns multiple tasks and verify a global semaphore or scheduler enforces the aggregate bound.

---

## Test

`ConcurrentValidationRequestsTest` runs a 4-node cluster repairing 5 tables with `concurrent_merkle_tree_requests=1` and uses ByteBuddy instrumentation to count concurrent validation requests, asserting the count never exceeds `MAX_REQUESTS * RF`.
