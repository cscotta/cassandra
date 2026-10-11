# Bug 1778: Fix partition and range deletes not triggering flush

**Commit**: adcb713d59 · **Ticket**: CASSANDRA-6655
**Subsystem**: db (Memtable, AtomicSortedColumns, DeletionInfo)
**Category**: Logic error in condition

---

## Root Cause

The Memtable tracked its size via `sizeDelta` from `addAllWithSizeDelta`, but the initial `sizeDelta` variable was declared inside the wrong scope — it was set to `previous.addAllWithSizeDelta(...)` without accounting for the deletion info size when a new empty `ColumnFamily` was freshly inserted. Additionally, `AtomicSortedColumns` never added the `DeletionInfo` data size to `sizeDelta` when a partition- or range-delete was applied, so those deletes did not inflate the tracked memtable size and therefore never triggered a flush.

### Before / After

```java
// BEFORE (Memtable)
if (previous == null) {
    previous = columnFamilies.putIfAbsent(..., empty);
    if (previous == null)
        previous = empty;
}
long sizeDelta = previous.addAllWithSizeDelta(cf, allocator, localCopyFunction, indexer);
currentOperations.addAndGet((cf.getColumnCount() == 0)
                            ? cf.isMarkedForDelete() ? 1 : 0
                            : cf.getColumnCount());
```

```java
// AFTER (Memtable)
long sizeDelta = 0;
if (previous == null) {
    previous = columnFamilies.putIfAbsent(..., empty);
    if (previous == null) {
        previous = empty;
        sizeDelta += empty.deletionInfo().dataSize();
    }
}
sizeDelta = previous.addAllWithSizeDelta(cf, allocator, localCopyFunction, indexer);
currentOperations.addAndGet(cf.getColumnCount() + (cf.isMarkedForDelete() ? 1 : 0) + cf.deletionInfo().rangeCount());
```

---

## How to Spot

Size-tracking accumulators that are only incremented for column writes but not for delete markers leave a blind spot. When reviewing memtable accounting, verify that every type of mutation — partition tombstone, range tombstone, and column write — contributes to both the size and the operation count.

---

## Test

No test added.
