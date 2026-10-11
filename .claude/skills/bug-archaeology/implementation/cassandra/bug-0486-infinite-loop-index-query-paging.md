# Bug 0486: Infinite Loop on Index Query Paging in Tables with Clustering

**Commit**: 6cd2d07d9a · **Ticket**: CASSANDRA-14242
**Subsystem**: `src/java/org/apache/cassandra/service/pager/`
**Category**: Logic error in condition

---

## Root Cause

`PartitionRangeQueryPager.nextPageReadCommand()` did not detect the case where the last returned key was the right boundary of the range and there were no more rows to return in that partition. Instead of returning `null` (exhausted), it kept building a new read command bounded by the same end-key, which executed and returned zero rows, causing the caller to loop forever. `AbstractQueryPager.fetchPage()` also blindly called `nextPageReadCommand().execute()` without checking for a `null` return that would signal exhaustion. Additionally, `MultiPartitionPager.state()` stored `Integer.MAX_VALUE` for `remainingInPartition` instead of the actual value, causing resumed pages to over-read.

### Before / After

```java
// BEFORE — fetchPage never checks whether nextPageReadCommand signals exhaustion
return Transformation.apply(
    nextPageReadCommand(pageSize).execute(consistency, clientState), pager);

// BEFORE — PartitionRangeQueryPager never terminates at range boundary
// (no check for lastReturnedKey == fullRange.right)
```

```java
// AFTER — null return means "exhausted"
ReadCommand readCommand = nextPageReadCommand(pageSize);
if (readCommand == null) {
    exhausted = true;
    return EmptyIterators.partition();
}
return Transformation.apply(readCommand.execute(consistency, clientState), pager);

// AFTER — explicit boundary check in PartitionRangeQueryPager
else if (lastReturnedKey.equals(fullRange.keyRange().right)
         && remainingInPartition() == 0 && lastReturnedRow == null) {
    return null;
}
```

---

## How to Spot

Paging loops that rely on an empty result to signal termination are fragile: if the underlying query re-issues a command that covers the same end-boundary, it may always produce an empty result without ever setting the `exhausted` flag. Review paging code for range-boundary conditions where `nextPageReadCommand` could legally return the same range twice. Also check that `PagingState` records the actual `remainingInPartition` rather than a sentinel like `Integer.MAX_VALUE`, which corrupts resumed sessions.

---

## Test

`IndexQueryPagingTest` was added to cover paging over secondary-index results on tables with clustering columns, asserting that paging terminates and returns the correct rows.
