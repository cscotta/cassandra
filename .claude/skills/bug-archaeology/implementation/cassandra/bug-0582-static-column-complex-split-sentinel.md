# Bug 0582: Static Complex Columns Sorted After Regular Columns in SELECT *

**Commit**: 236c47e65c · **Ticket**: CASSANDRA-14638
**Subsystem**: src/java/org/apache/cassandra/db/Columns.java
**Category**: Incorrect filtering / wrong query result

---

## Root Cause

`Columns.findFirstComplexIdx` used a single sentinel `FIRST_COMPLEX` column of kind `REGULAR` as the lower-bound marker for `BTree.ceilIndex` to locate where complex (collection) columns begin in the sorted tree. When the column set contained only `STATIC` columns, the regular-kind sentinel sorted after all static columns (since static columns sort before regular columns in `ColumnDefinition.compareTo`), so `ceilIndex` returned an index past all static columns — meaning all static columns were considered "simple" and none were considered "complex." `SELECT *` then iterated simple columns before complex ones incorrectly, reversing the expected alphabetical order for static collection columns.

### Before / After

```java
// BEFORE — single sentinel, always REGULAR kind
public static final ColumnDefinition FIRST_COMPLEX =
    new ColumnDefinition("", "", ..., SetType, NO_POSITION, Kind.REGULAR);

return BTree.ceilIndex(tree, Comparator.naturalOrder(), FIRST_COMPLEX);
```

```java
// AFTER — separate sentinels for static vs. regular column sets
private static final ColumnDefinition FIRST_COMPLEX_STATIC =
    new ColumnDefinition("", "", ..., SetType, NO_POSITION, Kind.STATIC);
private static final ColumnDefinition FIRST_COMPLEX_REGULAR =
    new ColumnDefinition("", "", ..., SetType, NO_POSITION, Kind.REGULAR);

return BTree.ceilIndex(tree, Comparator.naturalOrder(),
    last.isStatic() ? FIRST_COMPLEX_STATIC : FIRST_COMPLEX_REGULAR);
```

---

## How to Spot

Any sentinel or boundary value used in a sorted-collection search must be drawn from the same domain (same kind/type category) as the elements being searched. When a collection can hold items of multiple incomparable kinds (static vs. regular columns), a single cross-kind sentinel will produce wrong split points. Review `ceilIndex` / `floorIndex` sentinel objects to confirm their sort key matches the set's actual element kind.

---

## Test

`ColumnsTest.testStaticColumns` and `testRegularColumns` construct mixed simple/complex column sets of each kind and assert that `simpleColumns()`, `complexColumns()`, and `selectOrderIterator()` return columns in the correct alphabetical order.
