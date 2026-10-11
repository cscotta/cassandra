# Bug 1105: Fix Columns.contains()

**Commit**: ffe53de8d4 · **Ticket**: CASSANDRA-10178
**Subsystem**: src/java/org/apache/cassandra/db/Columns.java
**Category**: Logic error in condition

---

## Root Cause

`Columns.contains(Columns other)` used `other.columns.length` and `columns.length` to compare sizes as a fast-path early-exit guard. The backing BTree array does not directly store one element per column — it contains internal tree nodes in addition to leaf values, so `array.length` over-counts the number of logical entries. When a large Columns object (e.g., 50 columns) was compared against a smaller subset (e.g., 8 columns), the raw array of the larger set could be shorter than the BTree array of the smaller set, causing the early return to incorrectly report `false` even when the subset relationship held.

### Before / After

```java
// BEFORE
if (other.columns.length > columns.length)
    return false;
```

```java
// AFTER
if (other.columnCount() > columnCount())
    return false;
```

---

## How to Spot

When comparing collection sizes as a guard, always use the logical size method (`size()`, `count()`, `columnCount()`) rather than the raw backing array length. Raw array length is an implementation detail of the underlying data structure and may not equal the number of elements it represents. Code reviews should flag any direct `.length` access on a field that is documented or named as a non-flat structure (BTree, packed array, etc.).

---

## Test

A new unit test `testContainsColumnsWithLargeNumberOfColumns` creates a 50-column set, takes an 8-column subset, and asserts `contains` returns `true`, directly reproducing the failure scenario.
