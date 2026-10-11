# Bug 0992: Anticompaction SSTable Replace Ordering

**Commit**: 0d51b65e32 · **Ticket**: CASSANDRA-10831
**Subsystem**: Compaction / Repair
**Category**: State not cleaned up

---

## Root Cause
`doAntiCompaction` was calling `markCompactedSSTablesReplaced` inside the per-SSTable loop immediately after each SSTable was anticompacted. This caused each individual SSTable to be removed from the data tracker and replaced before the full set of anticompacted outputs was accumulated. If any subsequent SSTable in the loop threw an exception, the prior replacements had already mutated the tracker state while the remaining original SSTables were never removed. Additionally, the outer `performAnticompaction` method released the `Ref` objects in `validatedForRepair` *after* calling `unmarkCompacting`, so a window existed where the SSTables were unmarked (available for other compactions) but the refs were not yet released.

### Before / After
```java
// BEFORE (inside per-SSTable loop)
anticompactedSSTables.addAll(repairedSSTableWriter.finish(repairedAt));
anticompactedSSTables.addAll(unRepairedSSTableWriter.finish(ActiveRepairService.UNREPAIRED_SSTABLE));
cfs.getDataTracker().markCompactedSSTablesReplaced(sstableAsSet, anticompactedSSTables, OperationType.ANTICOMPACTION);

// Also: refs released AFTER unmarkCompacting
cfs.getDataTracker().notifySSTableRepairedStatusChanged(mutatedRepairStatuses);
cfs.getDataTracker().unmarkCompacting(Sets.union(nonAnticompacting, mutatedRepairStatuses));
validatedForRepair.release(Sets.union(nonAnticompacting, mutatedRepairStatuses));

// AFTER (single call after full loop completes)
successfullyAntiCompactedSSTables.add(sstable); // accumulate inside loop
// ...after loop:
cfs.getDataTracker().markCompactedSSTablesReplaced(successfullyAntiCompactedSSTables, anticompactedSSTables, OperationType.ANTICOMPACTION);

// Also: refs released BEFORE unmarkCompacting
validatedForRepair.release(Sets.union(nonAnticompacting, mutatedRepairStatuses));
cfs.getDataTracker().notifySSTableRepairedStatusChanged(mutatedRepairStatuses);
cfs.getDataTracker().unmarkCompacting(Sets.union(nonAnticompacting, mutatedRepairStatuses));
```

---

## How to Spot
When a loop accumulates results before registering them with shared mutable state, look for the registration call being placed inside the loop rather than after it completes. In compaction and repair paths, any call to `markCompactedSSTablesReplaced` or similar tracker-mutation methods inside a per-SSTable iteration is a strong signal that partial failure will leave the tracker in an inconsistent state. Also check that `Ref.release()` is ordered before the corresponding `unmarkCompacting` to prevent double-access to SSTables between the unmark and the release.

---

## Test
No test added.
