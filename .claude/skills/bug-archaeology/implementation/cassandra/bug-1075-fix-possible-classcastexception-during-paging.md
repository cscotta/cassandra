# Bug 1075: Fix Possible ClassCastException During Paging

**Commit**: 0c1432ac7b · **Ticket**: CASSANDRA-10352
**Subsystem**: src/java/org/apache/cassandra/service/pager/SliceQueryPager.java
**Category**: Missing null / bounds check

---

## Root Cause

`SliceQueryPager` unconditionally cast the `cellName` field from a `PagingState` to `CellName`, relying on a comment asserting that an empty `Composites.EMPTY` value would never legitimately appear there. However, when a `MultiPartitionPager` hands off between sub-pagers for a `DISTINCT` query involving static columns and an `IN` on the partition key, it passes a `PagingState` with an empty `cellName` to signal "start from the beginning of this partition." The cast then threw a `ClassCastException` at runtime. The fix guards the cast with a `hasRemaining()` check, leaving `lastReturned` null when the cell name is empty.

### Before / After

```java
// BEFORE
// The only case where this could be a non-CellName Composite is if it's Composites.EMPTY, but that's not
// valid for PagingState.cellName, so we can safely cast to CellName.
lastReturned = (CellName) cfm.comparator.fromByteBuffer(state.cellName);
```

```java
// AFTER
// The cellname can be empty if this is used in a MultiPartitionPager and we're supposed to start reading
// this row (because the previous page has exhausted the previous pager). See #10352 for details.
if (state.cellName.hasRemaining())
    lastReturned = (CellName) cfm.comparator.fromByteBuffer(state.cellName);
```

---

## How to Spot

A comment that says "this case cannot happen" should be treated as a hypothesis, not a guarantee, especially when the class is reused in contexts the original author may not have considered. Reviewers should trace all callers of a method that performs an unchecked cast and verify that every code path into it respects the precondition that the comment asserts.

---

## Test

No test added.
