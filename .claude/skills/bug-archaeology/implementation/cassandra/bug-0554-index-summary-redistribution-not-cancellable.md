# Bug 0554: Index Summary Redistribution Cannot Be Cancelled

**Commit**: 1d615a5ca1 · **Ticket**: CASSANDRA-15045
**Subsystem**: `src/java/org/apache/cassandra/db/compaction/CompactionManager.java`, `src/java/org/apache/cassandra/io/sstable/IndexSummaryManager.java`
**Category**: Missing null / bounds check

---

## Root Cause

`CompactionManager.interruptCompactionFor` checked `info.getCFMetaData()` against a set of target column families to decide whether to stop a compaction holder. Index summary redistribution runs as a compaction-like task but is a global operation that spans all keyspaces and tables — it reports a `null` CFMetaData. The null was never matched by `Iterables.contains`, so the stop signal was never delivered and the redistribution ran to completion even when cancellation was requested. The fix adds an explicit null check: a `null` CFMetaData is interpreted as "global" and always receives the stop signal.

### Before / After

```java
// BEFORE
if (Iterables.contains(columnFamilies, info.getCFMetaData()))
    compactionHolder.stop();
```

```java
// AFTER
// cfmetadata is null for index summary redistributions which are 'global'
if (info.getCFMetaData() == null || Iterables.contains(columnFamilies, info.getCFMetaData()))
    compactionHolder.stop();
```

---

## How to Spot

When iterating active compaction holders to signal cancellation, verify that every possible `getCFMetaData()` return value (including `null` for cross-table tasks) is handled. A contains-check on a collection of non-null items will silently miss any entry whose key is `null`.

---

## Test

A new `testCancelIndexInterrupt` test was added to `IndexSummaryManagerTest` that calls `interruptCompactionFor` against a specific CFS while an index-summary redistribution is running and asserts that it terminates promptly.
