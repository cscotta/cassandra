# Bug 0885: Incremental repair marks too many SSTables as repairing, enabling concurrent overlap

**Commit**: 3c8421a330 · **Ticket**: CASSANDRA-11696
**Subsystem**: Repair / Anticompaction
**Category**: Race condition / missing synchronization

---

## Root Cause
During incremental repair, `CompactionManager.doValidationCompaction()` called `ActiveRepairService.ParentRepairSession.addSSTables()` on every invocation — once per RepairSession. In a multi-vnode cluster (e.g. 256 vnodes, RF=3 → 768 RepairSessions) this meant the same set of SSTables was added to the parent repair session 768 times, and the overlap check against `currentlyRepairing()` was performed *after* the sstable refs had already been taken, so the guard was ineffective. Additionally, new SSTables flushed *during* the repair would be added to subsequent RepairSession calls, pulling in data that was written after the repair started. The fix introduces `markSSTablesRepairing()`, which performs a single idempotent snapshot of unrepaired SSTables at first invocation per CF and refuses further invocations, and renames the retrieval method to `getActiveRepairedSSTableRefsForAntiCompaction()` to make its semantics explicit.

### Before / After
```java
// BEFORE — addSSTables called on every RepairSession, no idempotence guard
ColumnFamilyStore.RefViewFragment refView = cfs.selectAndReference(UNREPAIRED_SSTABLES);
sstables = refView.refs;
// overlap check *after* refs already captured — race
if (!Sets.intersection(currentlyRepairing, Sets.newHashSet(refView.sstables)).isEmpty())
    throw new RuntimeException("Cannot start multiple repair sessions over the same sstables");
ActiveRepairService.instance.getParentRepairSession(validator.desc.parentSessionId)
        .addSSTables(cfs.metadata.cfId, refView.sstables);

// AFTER — idempotent markSSTablesRepairing, overlap check inside the guard
prs.markSSTablesRepairing(cfs.metadata.cfId, validator.desc.parentSessionId);
sstables = cfs.selectAndReference(UNREPAIRED_SSTABLES).refs;
```

```java
// NEW METHOD — marks once per CF, throws if overlap detected
public synchronized void markSSTablesRepairing(UUID cfId, UUID parentSessionId) {
    if (!marked.contains(cfId)) {
        List<SSTableReader> sstables = columnFamilyStores.get(cfId)
                .select(UNREPAIRED_SSTABLES).sstables;
        if (!Sets.intersection(currentlyRepairing(cfId, parentSessionId),
                               Sets.newHashSet(sstables)).isEmpty())
            throw new RuntimeException("Cannot start multiple repair sessions over the same sstables");
        addSSTables(cfId, sstables);
        marked.add(cfId);
    }
}
```

---

## How to Spot
Any repair or compaction code that accumulates SSTable sets across multiple invocations without an idempotence guard is vulnerable to double-marking. Look for `addSSTables`-style methods called in loops over sessions/ranges and verify they are protected by a `Set<UUID> marked` or equivalent guard. Also watch for overlap-detection checks that happen after a mutable shared collection has already been modified.

---

## Test
`ActiveRepairServiceTest.testAddingMoreSSTables()` was added to verify that a second `markSSTablesRepairing` call on a CF whose SSTables overlap with an ongoing repair throws an exception, and that the first repair session still sees only its original SSTables afterward.
