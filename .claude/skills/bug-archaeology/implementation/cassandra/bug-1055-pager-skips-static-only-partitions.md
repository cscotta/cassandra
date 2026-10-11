# Bug 1055: Pager Skips Partitions Containing Only Static Column Data

**Commit**: 328916d159 · **Ticket**: CASSANDRA-10381
**Subsystem**: Query / Paging
**Category**: Incorrect filtering / wrong query result

---

## Root Cause
`AbstractQueryPager.discardHead()` counted consumed rows by calling `counter.live()`, which only counted non-static cells. When a partition contained exclusively static-column data, `counter.live()` returned 0, so the pager's row counter did not advance. This meant the pager considered the partition not yet returned and would re-issue the same partition on the next page, producing duplicate results or an infinite loop.

In `SliceQueryPager` and `RangeSliceQueryPager`, `needsOffsetting()` (used to skip already-returned rows between pages) called `firstNonStaticCell()` to find the last-returned anchor. For a static-only partition `firstNonStaticCell()` returns `null`, and without a null guard the method proceeded to call `.name()` on `null` and crashed, or compared the null cell against `lastReturnedName` incorrectly.

The fixes: (1) in `AbstractQueryPager.discardHead()`, treat `live == 0 && !staticCells.isEmpty()` as `live = 1`; (2) in both pager subclasses, return `true` (partition already returned, skip it) immediately when `firstNonStaticCell` is `null`.

### Before / After
```java
// BEFORE – AbstractQueryPager.discardHead(): static-only row not counted
return Math.min(counter.live(), toDiscard);

// AFTER – static-only row counted as 1
int live = counter.live();
if (live == 0 && !staticCells.isEmpty())
    live = 1;
return Math.min(live, toDiscard);

// BEFORE – SliceQueryPager.needsOffsetting(): NPE / wrong result when no regular cell
Cell firstCell = isReversed() ? lastCell(first.cf) : firstNonStaticCell(first.cf);
return !first.cf.deletionInfo().isDeleted(firstCell) && firstCell.isLive(...) && ...;

// AFTER – null guard skips static-only partitions that were already returned
Cell firstCell = isReversed() ? lastCell(first.cf) : firstNonStaticCell(first.cf);
if (firstCell == null)
    return true;
```

---

## How to Spot
Any pager that tracks progress via "non-static live cells seen" will miscount or crash for partitions that are logically present but contain only static data. When reviewing paging code, explicitly ask: what happens if `firstNonStaticCell()` returns null? And verify that all row-count accumulators distinguish between "row with only static columns" and "empty row". Both scenarios must increment the page counter by exactly 1.

---

## Test
No new test file was added in this commit; the fix is covered by existing paging integration tests.
