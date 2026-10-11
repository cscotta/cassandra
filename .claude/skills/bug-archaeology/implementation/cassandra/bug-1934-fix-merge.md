# Bug 1934: Off-by-One in CompositesSearcher Column Limit Check

**Commit**: 1ff0d8e9f6 · **Ticket**: n/a
**Subsystem**: src/java/org/apache/cassandra/db/index/composites/CompositesSearcher.java
**Category**: Off-by-one

---

## Root Cause

`CompositesSearcher` checked `columnsCount > limit` to decide whether to stop fetching and return accumulated results. Using strict greater-than means the searcher would return one extra column (the `limit`-th column) before stopping, producing results with exactly `limit + 1` columns rather than `limit`. The fix changes the comparison to `columnsCount >= limit`, so the return is triggered as soon as the count reaches the limit.

### Before / After

```java
// BEFORE
if (columnsCount > limit)
    return makeReturn(currentKey, data);
```

```java
// AFTER
if (columnsCount >= limit)
    return makeReturn(currentKey, data);
```

---

## How to Spot

In any loop that accumulates items up to a limit, verify whether the stopping condition should be `>` or `>=`. The phrasing "did we get more columns than needed" suggests `>`, but the intent of a user-facing limit is "stop when we have enough," which requires `>=`.

---

## Test

No test added.
