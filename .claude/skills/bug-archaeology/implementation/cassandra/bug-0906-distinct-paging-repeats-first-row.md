# Bug 0906: DISTINCT Paging Repeats First Row When Partition Changes

**Commit**: f5baa9c712 · **Ticket**: CASSANDRA-11679
**Subsystem**: Query pager / RangeSliceQueryPager
**Category**: Incorrect filtering / wrong query result

---

## Root Cause
`RangeSliceQueryPager.isPreviouslyReturnedPartition` is responsible for deciding whether the leading row of the next page was already sent in the previous page. For DISTINCT queries, the pager only needs to compare partition keys — there is no need to inspect individual cell names. However, the method fell through to the cell-level comparison logic, which relies on `lastReturnedName` being set. For DISTINCT queries `lastReturnedName` is never set (there are no regular cells), so the guard `firstCell == null` would return `true` from the static-column branch, incorrectly marking a genuine new partition as already returned and skipping it. The fix short-circuits the method immediately after the key equality check when the query is DISTINCT.

### Before / After
```java
// BEFORE
if (lastReturnedKey == null || !lastReturnedKey.equals(first.key))
    return false;
// falls through to cell-name comparison — wrong for DISTINCT

// AFTER
if (lastReturnedKey == null || !lastReturnedKey.equals(first.key))
    return false;
if (isDistinct())
    return true;   // same key → skip it, no cell comparison needed
// cell-name comparison only for non-DISTINCT queries
```

---

## How to Spot
Pager `isAlreadyPaged` / `isPreviouslyReturned` logic usually has separate code paths for DISTINCT vs. non-DISTINCT queries because DISTINCT only advances by partition key, never by cell name. During review, verify that any cell-name (`lastReturnedName`) comparison is guarded by a check that the query actually selects regular columns. A `null` cell obtained from `firstNonStaticCell` in a DISTINCT query is a reliable signal that the cell-comparison branch is being reached in a context where it should not be.

---

## Test
No test added (CHANGES.txt updated only).
