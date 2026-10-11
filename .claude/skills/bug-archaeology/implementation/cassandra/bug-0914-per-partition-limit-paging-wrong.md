# Bug 0914: PER PARTITION LIMIT ignored on subsequent pages

**Commit**: b2d8e88217 · **Ticket**: CASSANDRA-11603
**Subsystem**: Query paging / DataLimits
**Category**: Incorrect filtering / wrong query result

---

## Root Cause
`SinglePartitionPager.nextPageReadCommand()` called `limits().forPaging(pageSize)` unconditionally, which created a new limit with `perPartitionLimit = Integer.MAX_VALUE` — discarding the original `PER PARTITION LIMIT` clause entirely on the second and subsequent pages. Simultaneously, `MultiPartitionPager.state()` recorded `Integer.MAX_VALUE` as `remainingInPartition` instead of the true count, so the paging state was also wrong when resuming across partitions.

### Before / After
```java
// BEFORE (SinglePartitionPager)
return command.forPaging(clustering, pageSize);
// → forPaging(int pageSize) drops per-partition limit

// AFTER
DataLimits limits = (lastReturned == null || command.isForThrift())
    ? limits().forPaging(pageSize)
    : limits().forPaging(pageSize, key(), remainingInPartition());
return command.forPaging(clustering, limits);

// BEFORE (MultiPartitionPager.state())
return new PagingState(key, rowMark, remaining, Integer.MAX_VALUE);

// AFTER
return new PagingState(key, rowMark, remaining, pagers[current].remainingInPartition());
```

---

## How to Spot
Whenever a pager constructs the next-page command by passing only a raw `pageSize` integer rather than the full `DataLimits` object, any `PER PARTITION LIMIT` carried by the original limits is silently reset to unlimited. Look for calls to `forPaging(int)` inside pager classes and verify that the variant accepting an existing `DataLimits` (or at least `remainingInPartition`) is used for continuation pages. The companion bug in `MultiPartitionPager.state()` is recognisable by any hardcoded `Integer.MAX_VALUE` passed as `remainingInPartition`.

---

## Test
No dedicated test added in this commit; the change was validated by the existing paging test suite and CASSANDRA-11603 integration scenarios.
