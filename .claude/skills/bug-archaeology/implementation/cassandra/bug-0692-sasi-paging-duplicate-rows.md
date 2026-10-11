# Bug 0692: SASI Paging Returns Duplicate Rows at Page Boundary

**Commit**: 9723db2717 · **Ticket**: CASSANDRA-13302
**Subsystem**: src/java/org/apache/cassandra/index/sasi/plan/QueryPlan.java
**Category**: Incorrect filtering / wrong query result

---

## Root Cause

SASI's `QueryPlan` iterator uses a key range to bound its scan, checking both the right bound (`keyRange.right`) and later the left bound. However, when paging, the left bound of the resumed range is *exclusive* (the last key of the previous page). The iterator advanced past the right-end guard but had no check for the left-end exclusivity, so the first key of a new page — which equals the last key returned on the previous page — was returned again, causing duplicate rows at every page boundary.

### Before / After

```java
// BEFORE — no check for exclusive left bound
if (!keyRange.right.isMinimum() && keyRange.right.compareTo(key) < 0)
    return endOfData();

try (UnfilteredRowIterator partition = controller.getPartition(key, executionController))
```

```java
// AFTER — skip the key that equals the exclusive left bound
if (!keyRange.right.isMinimum() && keyRange.right.compareTo(key) < 0)
    return endOfData();

if (!keyRange.inclusiveLeft() && key.compareTo(keyRange.left) == 0)
    continue;

try (UnfilteredRowIterator partition = controller.getPartition(key, executionController))
```

---

## How to Spot

Any secondary index iterator that advances using a token/key range should check *both* bound inclusivity flags, not just the right boundary. A missing `inclusiveLeft()` guard is especially dangerous when the left bound comes from a paging state (where it is always exclusive), because it will re-emit the boundary key on every subsequent page. Look for range-bounded `while`/`for` loops in index iterators that test only `right` and not `left`.

---

## Test

`SASICQLTest` was extended with a multi-page SELECT using SASI and an explicit `LIMIT` that forces pagination, asserting that all returned rows are unique across pages.
