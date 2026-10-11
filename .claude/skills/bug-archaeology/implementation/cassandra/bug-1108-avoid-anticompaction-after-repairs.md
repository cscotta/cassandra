# Bug 1108: Avoid Anticompaction After Non-Global Repairs

**Commit**: 842f1509d4 · **Ticket**: CASSANDRA-9142
**Subsystem**: src/java/org/apache/cassandra/service/ActiveRepairService.java
**Category**: Incorrect filtering / wrong query result

---

## Root Cause

Anticompaction after repair is only meaningful for global (cluster-wide) repairs — its purpose is to segregate repaired from unrepaired SSTables so that future incremental repairs are efficient. When a repair is scoped to specific data-centers or hosts (non-global), anticompaction incorrectly marks those SSTables as repaired across the full repair-time range, which can cause data that was not actually verified to be treated as repaired. The `ParentRepairSession` did not track whether a repair was global, so `doAntiCompaction` was always triggered. The fix adds an `isGlobal` flag (derived from whether the repair has DC or host filters) and a new `PREPARE_GLOBAL_MESSAGE` type to safely propagate that flag to peers, including old nodes that always default to global behaviour.

### Before / After

```java
// BEFORE
public void registerParentRepairSession(..., boolean isIncremental) {
    parentRepairSessions.put(..., new ParentRepairSession(..., isIncremental, ...));
}
// doAntiCompaction always proceeds
```

```java
// AFTER
public void registerParentRepairSession(..., boolean isIncremental, boolean isGlobal) {
    parentRepairSessions.put(..., new ParentRepairSession(..., isIncremental, isGlobal, ...));
}
// doAntiCompaction early-returns for non-global sessions
if (!prs.isGlobal) {
    logger.info("Not a global repair, will not do anticompaction");
    removeParentRepairSession(parentRepairSession);
    return Futures.immediateFuture(Collections.emptyList());
}
```

---

## How to Spot

When a post-processing step (anticompaction, cleanup, promotion) is only valid under a specific scope or precondition, encode that precondition as a first-class field in the session/state object and guard the step with it. Review any repair or compaction post-step that does not first inspect the repair scope. Additionally, `getRepairedAt()` now returns `UNREPAIRED_SSTABLE` for non-global sessions, preventing SSTable timestamps from being set as if repaired when they were not.

---

## Test

Existing tests (`LeveledCompactionStrategyTest`, `LocalSyncTaskTest`) are updated to pass the new `isGlobal` parameter. No new dedicated test for the anticompaction-skip path is added.
