# Bug 1364: Fix DISTINCT queries w/ limits/paging and tombstoned partitions

**Commit**: dd62f7bf79 · **Ticket**: CASSANDRA-8490
**Subsystem**: src/java/org/apache/cassandra/db/filter/SliceQueryFilter.java, db/ColumnFamilyStore.java, db/DataRange.java, service/StorageProxy.java
**Category**: Incorrect filtering / wrong query result

---

## Root Cause

`SELECT DISTINCT` on partition-key-only columns is answered by reading at most one cell per partition. When all cells in a partition were tombstoned, the partition still counted toward the query's `LIMIT` because the row-counting code treated any row returned from storage as a live row — it had no way to distinguish a tombstone-only partition from one with live data. Under paging, this caused pages to be "used up" by ghost partitions, and clients could receive fewer real rows than the requested limit or, worse, receive empty pages when live data remained.

The fix introduces a sentinel value `IGNORE_TOMBSTONED_PARTITIONS = -2` on `SliceQueryFilter.compositesToGroup`. When set, the coordinator and local store skip counting a partition toward the limit if all its data consists solely of tombstones, effectively making tombstone-only partitions invisible to `DISTINCT` queries.

### Before / After

```java
// BEFORE — always increments matched count, even for all-tombstone partitions
rows.add(new Row(rawRow.key, data));
matched++;
```

```java
// AFTER — only counts partition if it has live data (or ignore-tombstones flag not set)
rows.add(new Row(rawRow.key, data));
if (!ignoreTombstonedPartitions || !data.hasOnlyTombstones(filter.timestamp))
    matched++;
```

---

## How to Spot

Whenever a query path applies a `LIMIT` or row-count gate, verify that the counting predicate is appropriate for the query type. `DISTINCT` queries on partition keys have no column data to validate liveness through the normal cell-count path; a reviewer should ask "what happens when a partition contains only expired or deleted data?" and check whether such partitions are explicitly excluded from the limit accounting.

---

## Test

No new test in this commit; covered by the ticket's regression test suite.
