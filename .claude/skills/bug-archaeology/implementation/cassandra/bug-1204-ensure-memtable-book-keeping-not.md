# Bug 1204: Memtable Memory Accounting Corrupted When Net Allocation Is Negative

**Commit**: b757db1484 · **Ticket**: CASSANDRA-9681
**Subsystem**: src/java/org/apache/cassandra/utils/memory/MemtableAllocator.java
**Category**: State not cleaned up

---

## Root Cause

`AtomicBTreeColumns.finish()` called `allocator.onHeap().allocate(heapSize, writeOp)` after a BTree merge even when `heapSize` was negative (i.e. the merge shrank the live set). `allocate()` asserted `size >= 0` and unconditionally incremented the pool's allocated counter, so a shrinking write both violated the assertion and permanently inflated the reported heap usage. The underlying `MemtablePool` API also conflated allocation and release into a single signed `adjustAcquired` method, making it easy to call the wrong operation. The fix introduces a separate `adjust(long size, OpOrder.Group)` method on `SubAllocator` that routes to `released(-size)` for negative values and to `allocate(size, opGroup)` for positive ones, and replaces the `adjustAcquired`/`adjustReclaiming` primitives with clearly named `allocated`, `acquired`, `released`, `reclaiming`, and `reclaimed` methods.

### Before / After

```java
// BEFORE – allocate() called with potentially negative heapSize
protected void finish() {
    allocator.onHeap().allocate(heapSize, writeOp); // crashes or corrupts when heapSize < 0
    reclaimer.commit();
}

// MemtablePool: single signed method; release path was inconsistent
void adjustAcquired(long size, boolean alsoAllocated) { ... }
```

```java
// AFTER – adjust() handles both growth and shrinkage safely
protected void finish() {
    allocator.onHeap().adjust(heapSize, writeOp);
    reclaimer.commit();
}

// SubAllocator: explicit positive-only adjust dispatcher
public void adjust(long size, OpOrder.Group opGroup) {
    if (size <= 0) released(-size);
    else           allocate(size, opGroup);
}
```

---

## How to Spot

Any call site that feeds an arithmetic delta (which can be negative) into a method named `allocate`, `acquire`, or any variant that internally asserts non-negative size is a latent corruption bug. Code review should flag every place where a delta derived from a before/after size difference is passed to a memory-tracking primitive. The fix is to introduce an `adjust` or `delta` method at the API boundary that dispatches to the appropriate signed operation.

---

## Test

`NativeAllocatorTest` extended to verify that `adjust(-10)` correctly decrements ownership, `adjust(+10)` increments it, and a direct call to `allocate(-10)` throws `AssertionError`.
