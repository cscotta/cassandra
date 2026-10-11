# Bug 1586: Fix Memtable Copy Bug Causing Range Tombstones to Be Missed

**Commit**: a14a01c924 · **Ticket**: CASSANDRA-7371
**Subsystem**: src/java/org/apache/cassandra/db/AtomicBTreeColumns.java
**Category**: State not cleaned up

---

## Root Cause

`AtomicBTreeColumns.addAllWithSizeDelta()` uses an optimistic CAS loop to merge incoming `DeletionInfo` into the current memtable state. On each retry of the loop, it called `cm.deletionInfo().copy().add(...)` — but the `copy()` call was applied to the source `DeletionInfo` before merging, not after. Because `RangeTombstoneList` stores composites by reference and `add()` mutates the list in place, the source `cm.deletionInfo()` was being corrupted on the first iteration of the loop. Subsequent CAS retries operated on the already-mutated (partially merged) source, causing some range tombstones to be silently lost.

### Before / After

```java
// BEFORE — source DeletionInfo mutated on each CAS retry
deletionInfo = current.deletionInfo.copy().add(cm.deletionInfo());
```

```java
// AFTER — source copied once outside the loop with a stable heap copy
if (inputDeletionInfoCopy == null)
    inputDeletionInfoCopy = cm.deletionInfo().copy(HeapAllocator.instance);
deletionInfo = current.deletionInfo.copy().add(inputDeletionInfoCopy);
```

---

## How to Spot

In CAS retry loops, any object that is read from an argument and then passed to a mutating method must be copied before the loop or its copy must survive across retries unchanged. Look for calls to mutating merge/add methods on parameters inside `while (true)` CAS patterns — if the parameter itself (not a local copy of it) is mutated, retries will operate on stale data.

---

## Test

A new `DeleteTest` integration test was added (`test/unit/org/apache/cassandra/cql3/DeleteTest.java`). It runs 500 iterations of insert → update → delete on multiple tables via the native transport and asserts that after each delete the row is fully absent from all tables.
