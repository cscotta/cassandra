# Bug 1142: Bug in Merging of Collections Across SSTables

**Commit**: 6818ba9ba1 · **Ticket**: CASSANDRA-10001
**Subsystem**: src/java/org/apache/cassandra/db/rows/Cell.java, src/java/org/apache/cassandra/db/rows/Row.java
**Category**: Wrong constant / default value

---

## Root Cause

`Cell.comparator` was declared as an instance field rather than a `static` field. When `Row.Merger` assembled complex cells from multiple SSTables it passed `ColumnData.comparator` (a comparator over `ColumnData` base type) to `MergeIterator.get()` instead of `Cell.comparator`. Because each `Cell` instance had its own `comparator` reference and the wrong comparator was being used for merge ordering, cells from different SSTables were not merged correctly, causing collection entries written in separate flushes to be lost on read.

### Before / After

```java
// BEFORE (Cell.java)
public final Comparator<Cell> comparator = (c1, c2) -> { ... };

// BEFORE (Row.java)
Iterator<Cell> cells = MergeIterator.get(complexCells, ColumnData.comparator, cellReducer);
```

```java
// AFTER (Cell.java)
public final static Comparator<Cell> comparator = (c1, c2) -> { ... };

// AFTER (Row.java)
Iterator<Cell> cells = MergeIterator.get(complexCells, Cell.comparator, cellReducer);
```

---

## How to Spot

Comparator fields that are logically type-level constants should always be `static`. A non-static comparator on an abstract class creates one instance per object, wastes memory, and — critically — can be shadowed or confused with a supertype comparator of a different generic parameter. When a merge or sort routine accepts a `Comparator<SubType>` but the only readily visible field is `Comparator<SuperType>`, verify which concrete type the merge actually operates on and that the correct, type-specific comparator is supplied.

---

## Test

Three new tests in `CollectionsTest`: `testMapWithLargePartition` (single SSTable, large map), `testMapWithTwoSStables` (entries split across two flushes), and `testSetWithTwoSStables` (set entries split across two flushes) — all verify the full collection is returned after a read that crosses SSTable boundaries.
