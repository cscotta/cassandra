# Bug 1228: Truncate Without Snapshot Can Return Corrupt Responses (2.1 Backport)

**Commit**: 6250065089 · **Ticket**: CASSANDRA-9388
**Subsystem**: src/java/org/apache/cassandra/db/ColumnFamilyStore.java
**Category**: Race condition / missing synchronization

---

## Root Cause

This is a backport of the same fix applied in commit b9a89a3acc to the 2.1 branch. During a truncate-without-snapshot, clean memtables were discarded by calling `memtable.setDiscarded()` directly and synchronously, without the `OpOrder.Barrier` that the normal flush path uses to ensure all in-flight reads have completed before the memtable's memory is released. A concurrent read that had already entered the memtable could therefore access freed memory, producing a corrupt response. The fix extracts the barrier-and-discard sequence into a `reclaim()` helper shared by both the truncate and flush paths.

### Before / After

```java
// BEFORE (truncate path — no read barrier)
memtable.cfs.replaceFlushed(memtable, null);
memtable.setDiscarded();
iter.remove();
```

```java
// AFTER
memtable.cfs.replaceFlushed(memtable, null);
reclaim(memtable);   // issues OpOrder.Barrier, then setDiscarded() asynchronously
iter.remove();
```

---

## How to Spot

Any time a resource that can be concurrently accessed is freed without an ordering barrier, reads or writes in flight can see invalid state. Reviewers should check that every code path that disposes of a shared structure goes through the same synchronisation step—here, the read `OpOrder.Barrier`—and be suspicious of copy-pasted disposal code that omits the barrier present in the "normal" path.

---

## Test

No test added.
