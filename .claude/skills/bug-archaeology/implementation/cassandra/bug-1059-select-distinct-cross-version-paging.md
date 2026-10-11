# Bug 1059: SELECT DISTINCT Breaks Mixed 2.2.2 / 3.0 Cluster Paging

**Commit**: 5d6455f29c · **Ticket**: CASSANDRA-10473
**Subsystem**: Read Path / Cross-Version Compatibility
**Category**: Incorrect filtering / wrong query result

---

## Root Cause
When a 3.0 coordinator paged a request through a legacy (pre-3.0) node, it adjusted the `DataRange` start bound so that the pre-3.0 node would not re-return the last row of the previous page. The adjustment logic checked whether `startBound` differed from the first slice start to decide whether to mark the range as non-inclusive, but it did not skip this adjustment for `SELECT DISTINCT` queries. For `SELECT DISTINCT`, slices are not comparable in the same way, so the condition `!startBound.bound.equals(slices.get(0).start())` could produce an incorrect inclusive/exclusive decision, causing rows to be silently skipped or re-emitted during pagination between 2.2.2 and 3.0 nodes.

### Before / After
```java
// BEFORE
if (startBound != LegacyLayout.LegacyBound.BOTTOM
        && !startBound.bound.equals(slices.get(0).start()))

// AFTER
if (!isDistinct
        && startBound != LegacyLayout.LegacyBound.BOTTOM
        && !startBound.bound.equals(slices.get(0).start()))
```

---

## How to Spot
Cross-version compatibility code that builds a `DataRange` for legacy nodes must explicitly distinguish `SELECT DISTINCT` from normal range reads. Whenever slice-level bound comparisons are used to decide inclusiveness, verify that the comparison is valid for the particular query type — `SELECT DISTINCT` uses a different slice layout that invalidates the bound-equality heuristic.

---

## Test
No test added.
