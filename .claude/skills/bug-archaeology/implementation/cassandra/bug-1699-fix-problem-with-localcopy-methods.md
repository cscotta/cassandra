# Bug 1699: Fix problem with localCopy methods after CASSANDRA-6689

**Commit**: 3bb87d1699 · **Ticket**: n/a (follow-up to CASSANDRA-6689)
**Subsystem**: src/java/org/apache/cassandra/db/ArrayBackedSortedColumns.java, src/java/org/apache/cassandra/db/CollationController.java
**Category**: Logic error in condition

---

## Root Cause

CASSANDRA-6689 refactored `Cell.localCopy()` to add a new two-argument overload `localCopy(ColumnFamilyStore, Allocator)`. The call sites in `ArrayBackedSortedColumns` and `CollationController` were written to use that two-arg form, but the correct overload for heap-copy operations is the one-argument `localCopy(Allocator)`. Passing the `ColumnFamilyStore` as the first argument caused the method to dispatch to the wrong variant, potentially using the wrong allocator strategy and producing cells that were not properly heap-resident.

### Before / After

```java
// BEFORE
cells[i++] = cell.localCopy(cfs, HeapAllocator.instance);
// ...
atom = ((Cell) atom).localCopy(cfs, HeapAllocator.instance);
// ...
newCf.addColumn(cell.localCopy(cfs, HeapAllocator.instance));
```

```java
// AFTER
cells[i++] = cell.localCopy(HeapAllocator.instance);
// ...
atom = ((Cell) atom).localCopy(HeapAllocator.instance);
// ...
newCf.addColumn(cell.localCopy(HeapAllocator.instance));
```

---

## How to Spot

When a refactor changes a method signature (adding or removing parameters), search all call sites for the old signature. Overload resolution can silently bind to a different method if the argument count still matches after the change, so always verify dispatch after altering overload sets.

---

## Test

No test added.
