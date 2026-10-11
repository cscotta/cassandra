# Bug 1438: Fix Dupes in DISTINCT Queries on Static Cols with Paging

**Commit**: 19c2f54b3a · **Ticket**: CASSANDRA-8108
**Subsystem**: src/java/org/apache/cassandra/service/pager/SliceQueryPager.java
**Category**: Incorrect filtering / wrong query result

---

## Root Cause

`SliceQueryPager.queryNextPage()` always fetched exactly `pageSize` rows by passing `pageSize` directly to `command.filter.withUpdatedCount()`. For a `DISTINCT` query on static columns the underlying filter already has `count = 1` because each partition contributes exactly one logical row. Passing a larger `pageSize` overrode that limit, causing the pager to fetch more rows than the query's semantic limit allowed. On the next page iteration the pager would re-read rows from the same partition, producing duplicate results.

### Before / After

```java
// BEFORE
SliceQueryFilter filter = command.filter.withUpdatedCount(pageSize);
```

```java
// AFTER
// Use the minimum of the filter's own count and the page size so that
// DISTINCT queries (filter.count == 1) are not over-fetched.
SliceQueryFilter filter = command.filter.withUpdatedCount(Math.min(command.filter.count, pageSize));
```

---

## How to Spot

Whenever a pager overrides a query filter's row count with an externally supplied page size, it should take the minimum of the two values rather than unconditionally replacing the filter's count. A query-level limit encodes semantic correctness (e.g., "one row per partition for DISTINCT"), whereas `pageSize` is a transport-level hint. Overwriting the former with the latter can violate result set semantics at page boundaries.

---

## Test

No test added.
