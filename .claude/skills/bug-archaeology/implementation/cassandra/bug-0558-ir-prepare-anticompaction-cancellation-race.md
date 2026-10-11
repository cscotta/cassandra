# Bug 0558: Incremental Repair Prepare Phase Does Not Respect Session Cancellation

**Commit**: 9bde713ee8 · **Ticket**: CASSANDRA-15027
**Subsystem**: `src/java/org/apache/cassandra/repair/consistent/`, `src/java/org/apache/cassandra/db/repair/`, `src/java/org/apache/cassandra/db/compaction/CompactionManager.java`
**Category**: Race condition / missing synchronization

---

## Root Cause

The incremental-repair (IR) prepare phase launched anti-compaction asynchronously but had no mechanism for the coordinator to signal cancellation down to the running `CompactionIterator`. If the session was failed or aborted by the coordinator while anti-compaction was in progress on a participant, the anti-compaction continued running to completion rather than stopping early. Additionally, `CoordinatorSession.handlePrepareResponse` would mark the entire session `FAILED` as soon as any single participant failed, discarding responses from still-PREPARING participants before they arrived. The fix threads a `BooleanSupplier isCancelled` through the entire prepare/anticompaction call chain, supplies it as `() -> session.getState() != PREPARING` in `LocalSessions`, overrides `CompactionIterator.isStopRequested()` to consult it, and restructures `handlePrepareResponse` to wait for all participants before transitioning state.

### Before / After

```java
// BEFORE — anticompaction had no cancellation hook
CompactionIterator getAntiCompactionIterator(...) {
    return new CompactionIterator(ANTICOMPACTION, scanners, controller, nowInSec, timeUUID, activeCompactions);
}
```

```java
// AFTER
CompactionIterator getAntiCompactionIterator(..., BooleanSupplier isCancelled) {
    return new CompactionIterator(ANTICOMPACTION, scanners, controller, nowInSec, timeUUID, activeCompactions) {
        public boolean isStopRequested() {
            return super.isStopRequested() || isCancelled.getAsBoolean();
        }
    };
}
```

---

## How to Spot

Any long-running asynchronous operation that is initiated from a stateful session should accept an explicit cancellation signal rather than relying solely on external compaction-manager stop requests. Look for `Futures.addCallback` chains that launch compaction work without a corresponding cancellation path — if the session state can become `FAILED` before the work finishes, the work will silently continue until natural completion.

---

## Test

A new `cancelledAntiCompaction` unit test in `PendingAntiCompactionTest` verifies that passing `() -> true` as `isCancelled` causes `performAnticompaction` to throw `CompactionInterruptedException`. A new `CoordinatorMessagingTest` class was also added covering the full prepare happy-path and various failure/timeout scenarios using mock messaging.
