# Bug 1227: Truncate Without Snapshot Can Return Corrupt Responses

**Commit**: b9a89a3acc · **Ticket**: CASSANDRA-9388
**Subsystem**: src/java/org/apache/cassandra/db/ColumnFamilyStore.java
**Category**: Race condition / missing synchronization

---

## Root Cause

During a truncate-without-snapshot, clean memtables were discarded by calling `memtable.setDiscarded()` directly and synchronously. This freed the memtable's memory immediately, without waiting for in-flight reads that had already entered the memtable to complete. A concurrent read could therefore observe freed or reused memory, producing a corrupt response. The normal flush path correctly issued an `OpOrder.Barrier` on the read ordering before calling `setDiscarded()`, ensuring all ongoing reads completed first; the truncate path accidentally bypassed this barrier. The fix extracts the barrier-and-discard logic into a shared `reclaim()` helper and calls it uniformly from both paths.

### Before / After

```java
// BEFORE (truncate path — no read barrier)
memtable.cfs.replaceFlushed(memtable, null);
memtable.setDiscarded();   // memory freed while reads may still be in-flight
iter.remove();
```

```java
// AFTER (uses the same reclaim helper as the flush path)
memtable.cfs.replaceFlushed(memtable, null);
reclaim(memtable);         // issues read barrier, then sets discarded asynchronously
iter.remove();
```

---

## How to Spot

Any time a resource that can be concurrently accessed is freed without an ordering barrier, reads or writes in flight can see invalid state. Reviewers should check that every code path that disposes of a shared structure goes through the same synchronisation step—here, the read `OpOrder.Barrier`—and be suspicious of copy-pasted disposal code that omits the barrier present in the "normal" path.

---

## Test

No test added.
