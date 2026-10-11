# Bug 0178: SAI Iterator Resources Not Closed on Query Timeout

**Commit**: 5ec0669bec · **Ticket**: CASSANDRA-19177
**Subsystem**: index/sai/disk, index/sai/plan
**Category**: Resource leak

---

## Root Cause

`IndexSearchResultIterator`, `QueryController`, and `StorageAttachedIndexSearcher` called `queryContext.checkpoint()` inside `finally` blocks. When a `QueryCancelledException` is thrown by the iterator machinery, the `finally` block fires and `checkpoint()` throws a second exception, which replaces the original and — crucially — bypasses any `close()` call on the underlying `PostingList` and `PrimaryKeyMap` file handles. The fix moves the single canonical `checkpoint()` call to `StorageAttachedIndexSearcher.ResultRetriever` (after each partition is read) and adds an explicit `FileUtils.closeQuietly(postingList, primaryKeyMap)` in `PostingListRangeIterator.computeNext`'s catch block to ensure resources are released even when an exception propagates.

### Before / After

```java
// BEFORE – IndexSearchResultIterator.computeNext
protected PrimaryKey computeNext()
{
    try { return union.hasNext() ? union.next() : endOfData(); }
    finally { context.checkpoint(); }   // double-throw swallows original; close() never runs
}
```

```java
// AFTER – checkpoint moved out of finally; explicit close on error
protected PrimaryKey computeNext()
{
    return union.hasNext() ? union.next() : endOfData();
}
// ...
// In PostingListRangeIterator.computeNext catch block:
FileUtils.closeQuietly(Arrays.asList(postingList, primaryKeyMap));
throw Throwables.cleaned(t);
```

---

## How to Spot

A `checkpoint()` call that throws a checked/unchecked exception inside a `finally` block suppresses the original exception and prevents subsequent cleanup code from running. During review, any `finally { someMethod(); }` pattern where `someMethod` itself can throw should be treated with suspicion; the cleanup logic that follows it will be unreachable.

---

## Test

No new test added; the existing SAI query timeout test suite covers the regression scenario.
