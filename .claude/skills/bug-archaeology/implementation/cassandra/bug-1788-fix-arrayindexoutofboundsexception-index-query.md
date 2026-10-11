# Bug 1788: Fix ArrayIndexOutOfBoundsException in Secondary Index Query (2.0 branch)

**Commit**: 8f17bbd0d3 · **Ticket**: CASSANDRA-6470
**Subsystem**: src/java/org/apache/cassandra/db
**Category**: Missing null / bounds check

---

## Root Cause

`DataRange.Paging.slicesForKey` used `columnStart` and `columnFinish` as slice boundaries whenever the query key matched the start or stop key of the paging range. When a secondary-index query wrapped the range, `columnStart` and/or `columnFinish` could be set to empty `Composite` values. Passing an empty `Composite` as a slice bound confused the downstream slice-construction logic, which allocated arrays based on the assumption that an assigned boundary was non-empty, causing an `ArrayIndexOutOfBoundsException`. The fix treats an empty composite the same as "no restriction" by adding `!columnStart.isEmpty()` / `!columnFinish.isEmpty()` guards before applying the boundary.

### Before / After

```java
// BEFORE
Composite newStart  = equals(startKey(), key) ? columnStart : null;
Composite newFinish = equals(stopKey(),  key) ? columnFinish : null;
```

```java
// AFTER
Composite newStart  = equals(startKey(), key) && !columnStart.isEmpty()  ? columnStart  : null;
Composite newFinish = equals(stopKey(),  key) && !columnFinish.isEmpty() ? columnFinish : null;
```

---

## How to Spot

Whenever a method accepts a typed value and uses it as a conditional boundary, verify that the "use it" branch also rejects the empty/default sentinel of that type, not just null.

---

## Test

No test added in this commit. Reproduction requires a secondary-index query that hits the boundary key of a paging range with an empty column start or finish.
