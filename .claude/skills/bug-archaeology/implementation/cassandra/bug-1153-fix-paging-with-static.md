# Bug 1153: Paging Returns Phantom Partition Containing Only Static Columns

**Commit**: 6aa8399053 · **Ticket**: CASSANDRA-9775
**Subsystem**: src/java/org/apache/cassandra/service/pager/AbstractQueryPager.java
**Category**: Incorrect filtering / wrong query result

---

## Root Cause

When a paged query resumed at a partition boundary and the resumed partition had no more regular rows but did have a static row, the pager would still emit that partition to callers. The upper layer interprets a partition that contains only a static row as a partition that is *entirely* static, which is wrong: the static data was already delivered on the previous page. The root cause is that `AbstractQueryPager` never checked whether the first partition on a new page was a continuation of the last partition on the previous page that had been exhausted of regular rows. The fix introduces `isPreviouslyReturnedPartition` and overrides `hasNext` to skip such empty continuations before surfacing the partition.

### Before / After

```java
// BEFORE – next() was called directly; no check for exhausted continuation
public RowIterator next() {
    RowIterator iter = super.next();
    DecoratedKey key = iter.partitionKey();
    ...
    return new RowPagerIterator(iter);
}
```

```java
// AFTER – hasNext() skips the exhausted first-partition continuation
if (isFirstPartition && isPreviouslyReturnedPartition(key) && !nextPartition.hasNext()) {
    nextPartition.close();
    nextPartition = null;
}
isFirstPartition = false;
```

---

## How to Spot

Any pager that calls `super.next()` without first checking whether the returned partition is a stale continuation is vulnerable to this class of bug. During review, look for paging iterators that do not distinguish between "this partition was started on the previous page" and "this is a brand-new partition". The presence of static columns makes the issue observable because an empty-regular-rows partition still has something to return, amplifying the incorrect result rather than silently returning an empty page.

---

## Test

No dedicated test added in this commit; covered by the broader CASSANDRA-9775 paging correctness work.
