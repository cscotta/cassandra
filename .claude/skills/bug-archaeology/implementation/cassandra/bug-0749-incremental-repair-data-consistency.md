# Bug 0749: Incremental repair data mixed with unrepaired data during compaction

**Commit**: 98d74ed998 · **Ticket**: CASSANDRA-9143
**Subsystem**: Repair / Compaction
**Category**: State not cleaned up

---

## Root Cause
Before this change, incremental repair used a two-state SSTable classification (repaired / unrepaired). After anti-compaction, SSTables covered by a repair session were immediately promoted to `isRepaired=true` even though the repair session had not yet committed. If the repair failed or was rolled back, those SSTables would remain marked as repaired, meaning subsequent reads could serve inconsistent data by mixing truly-repaired and only-tentatively-repaired files in the same compaction. The fix introduces a third state — **pending repair** — identified by a session UUID in SSTable metadata. SSTables stay in the pending state until the repair session definitively commits or fails, at which point they are promoted to repaired or demoted to unrepaired via a dedicated `PendingRepairManager` compaction path. Compaction is prevented from mixing SSTables from different pending sessions or between pending and non-pending buckets.

### Before / After
```java
// BEFORE — anti-compaction immediately marks SSTables as repaired
// (no pending state; failure left repaired=true on failed-session SSTables)

// AFTER — SSTables enter a pending state keyed by session UUID
// SSTableMetadata gains a pendingRepair UUID field
// PendingRepairManager routes compaction per session
// On commit: pending -> repaired; on failure: pending -> unrepaired
if (isPending && !pendingRepair.equals(sstable.getSSTableMetadata().pendingRepair))
    throw new UnsupportedOperationException(
        "You can't compact sstables from different pending repair sessions");
```

---

## How to Spot
Any repair protocol that transitions SSTable state at the *start* of a repair rather than at its *commit* is vulnerable to leaving stale state on failure. The safe pattern is optimistic pending-state tagging (with a session ID) followed by an atomic finalization step; intermediate state must never be observable as final state by compaction or reads.

---

## Test
Extensive test coverage added: `PendingRepairManagerTest`, `CompactionStrategyManagerPendingRepairTest`, `AbstractPendingRepairTest`, `CoordinatorSessionTest`, `LocalSessionTest`, `PendingAntiCompactionTest`, and updated `AntiCompactionTest` and `ActiveRepairServiceTest`.
