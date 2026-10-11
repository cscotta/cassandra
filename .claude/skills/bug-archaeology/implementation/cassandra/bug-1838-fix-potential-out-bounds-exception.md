# Bug 1838: Fix Potential Out-of-Bounds Exception During Paging

**Commit**: 3c9760bdb9 · **Ticket**: CASSANDRA-6333
**Subsystem**: src/java/org/apache/cassandra/service/pager
**Category**: Missing null / bounds check

---

## Root Cause

`AbstractQueryPager.fetchPage()` assumed that `StorageProxy.getRangeSlice()` returned at most `currentPageSize` live cells. In practice, `getRangeSlice()` does not trim its results, so it can return more live cells than requested. The pager then called `discardFirst()` or `discardLast()` expecting to remove exactly 1 cell at a time, but when the excess spanned multiple rows the existing `discardLast(List<Row>)` only looked at the *last* row in the list, causing an out-of-bounds array access when trying to discard more cells than that row contained. The fix has two parts: (1) trim any excess above `currentPageSize` immediately after fetching; (2) generalise `discardLast` to accept a count `toDiscard` and loop across rows until the required number of live cells is removed.

### Before / After

```java
// BEFORE — no trimming, discardLast only handled a single row
int liveCount = getPageLiveCount(rows);
remaining -= liveCount;
// ... discardLast touched only rows.get(rows.size()-1), could go OOB
```

```java
// AFTER — trim excess, then generalised discard
int liveCount = getPageLiveCount(rows);
if (liveCount > currentPageSize) {
    rows = discardLast(rows, liveCount - currentPageSize);
    liveCount = currentPageSize;
}
remaining -= liveCount;
// discardLast(rows, toDiscard) now loops across rows until toDiscard live cells removed
```

---

## How to Spot

When a caller passes a "limit" to a query layer that may silently over-return results, always validate and trim the response before feeding it into downstream logic that assumes the limit was respected. Any arithmetic on collection size that doesn't account for over-sized responses can produce index errors.

---

## Test

No test added.
