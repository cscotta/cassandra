# Bug 0927: AbstractQueryPager Loops Forever Discarding Cells When Page Contains Only Deletions

**Commit**: ea9b42e7d7 · **Ticket**: CASSANDRA-11467
**Subsystem**: Query paging / COMPACT STORAGE
**Category**: Logic error in condition

---

## Root Cause
`AbstractQueryPager.discardLast()` was asked to discard `toDiscard` live cells from the end of a column family. It computed `liveCount` first, then started a `ColumnCounter` loop expecting to stop after `liveCount - toDiscard` live cells. When a COMPACT table with no clustering columns returned a page in which every cell was a deletion (so `liveCount == 0` and therefore `liveCount == toDiscard == 0`), the stopping condition `liveCount - toDiscard` equalled zero from the start. The loop tried to stop immediately but had no guard to detect that there was nothing to iterate, causing it to spin without making progress. The fix short-circuits early when `liveCount == toDiscard`, returning `toDiscard` directly and skipping the iteration entirely.

### Before / After
```java
// BEFORE — no guard when liveCount == toDiscard
int liveCount = columnCounter().countAll(cf).live();
ColumnCounter counter = columnCounter();
while (iter.hasNext()) { ... }

// AFTER — early return prevents infinite loop / wrong iteration
int liveCount = columnCounter().countAll(cf).live();
if (liveCount == toDiscard)
    return toDiscard;
ColumnCounter counter = columnCounter();
while (iter.hasNext()) { ... }
```

---

## How to Spot
In any paging discard loop, verify what happens when `liveCount == toDiscard` (i.e., nothing to keep). If the loop's termination condition is expressed as `liveCount - toDiscard` and both quantities are zero, the counter never advances and the loop is infinite or vacuously correct only by accident. Always add a pre-loop guard for the degenerate case. COMPACT tables without clustering columns are especially prone to this because they produce pages that may contain only tombstones.

---

## Test
No test added.
