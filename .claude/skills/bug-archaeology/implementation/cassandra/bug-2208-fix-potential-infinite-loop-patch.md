# Bug 2208: Infinite Loop in Thrift get_count When Count Equals Page Size

**Commit**: 95fb613bf9 · **Ticket**: CASSANDRA-4833
**Subsystem**: src/java/org/apache/cassandra/thrift/CassandraServer.java
**Category**: Logic error in condition

---

## Root Cause

The Thrift `get_count` implementation paginates through columns using `predicate.slice_range.count` both as the user-requested limit and as the per-page fetch size, mutating the same field in place. When the total requested count was an exact multiple of the page size, the pagination loop would zero out `requestedCount` but never stop: `columns.size() < predicate.slice_range.count` would be false (the last full page) and `requestedCount == 0` was mistakenly checked before the update. The fix separates the remaining count (`remaining`) from the original `requestedCount`, and also enforces a minimum fetch of 2 columns per page to avoid the loop stalling when `remaining` drops to 1.

### Before / After

```java
// BEFORE
int requestedCount = predicate.slice_range.count;
// ...
predicate.slice_range.count = Math.min(pageSize, requestedCount);
// ...
int newColumns = pages == 0 || !firstName.equals(...) ? columns.size() : columns.size() - 1;
totalCount += newColumns;
requestedCount -= newColumns;
// ...
if (requestedCount == 0 || columns.size() < predicate.slice_range.count)
    break;
```

```java
// AFTER
final int requestedCount = predicate.slice_range.count;
int remaining = requestedCount;
// ...
predicate.slice_range.count = Math.min(pageSize, Math.max(2, remaining)); // fetch at least two columns
// ...
totalCount += newColumns;
if (totalCount > requestedCount)
    return requestedCount;
remaining -= newColumns;
// ...
if (remaining == 0 || columns.size() < predicate.slice_range.count)
    break;
```

---

## How to Spot

When a loop variable is decremented toward zero and tested as the stop condition, check that the decrement always uses the same variable that is tested—not a copy or related field. Also watch for mutation of request parameters (`predicate.slice_range.count`) being reused simultaneously as both a per-page size and a remaining-count tracker.

---

## Test

Test verifies `get_count` with 3050 columns against counts at the page boundary (1024), one above (1025), equal to column count (3050), one above column count (3051), and well above column count (4000).
