# Bug 0826: Paging sends exclusive start to 2.x nodes (3.0 branch)

**Commit**: 932f3ebbe9 · **Ticket**: CASSANDRA-11195
**Subsystem**: CQL query execution / mixed-version paging
**Category**: Wrong serialization / deserialization

---

## Root Cause
This is the cassandra-3.0 branch backport of the same bug fixed in commit c11c7d73d0 (bug-0825). When a 3.0.x coordinator serialises a resuming range scan for a 2.x replica, it passed `Slice.Bound.exclusiveStartOf(lastReturned)` as the start bound, but 2.x nodes cannot express or respect exclusive starts. The fix switches to `inclusiveStartOf` and unconditionally adds 1 to the request limit, mirroring the pre-3.0 protocol convention that the response always echoes the start cell. On the response path the non-compound branch was independently filtering results with `filter.filter(...)` when it should have applied the normal slice filter uniformly.

### Before / After
```java
// BEFORE
Slice.Bound newStart = Slice.Bound.exclusiveStartOf(lastReturned);
int maxResults = rangeCommand.limits().count() + (metadata.isCompound() ? 0 : 1);
// response path for non-compound:
iterator = filter.filter(partition.sliceableUnfilteredIterator(...));

// AFTER
Slice.Bound newStart = Slice.Bound.inclusiveStartOf(lastReturned);
int maxResults = rangeCommand.limits().count() + 1;
// response path:
iterator = filter.filter(partition.sliceableUnfilteredIterator(...)); // always filter
```

---

## How to Spot
When backporting a paging bug fix across branches, verify that the response-side deserialization path is also corrected; fixes applied only to the serialization side leave the response handler free to re-introduce the skipped row. Code that branches on `isCompound()` to decide how many extra rows to fetch or how to filter results is a reliable indicator that the 2.x compatibility layer has accumulated technical debt.

---

## Test
No new test added with this backport commit.
