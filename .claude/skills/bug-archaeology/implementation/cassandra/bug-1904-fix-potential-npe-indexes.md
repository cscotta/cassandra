# Bug 1904: NPE in composites secondary index stale-entry check

**Commit**: 58eebc9a97 · **Ticket**: CASSANDRA-6098
**Subsystem**: src/java/org/apache/cassandra/db/index/composites/
**Category**: Missing null / bounds check

---

## Root Cause

`CompositesSearcher` fetches the base-table row for each secondary-index entry to decide whether the entry is stale. The fetch can legitimately return `null` when the row has been deleted or has never existed on this node. The `isStale()` implementations in both `CompositesIndexOnClusteringKey` and `CompositesIndexOnPartitionKey` originally guarded against `null` inline (`data == null || data.hasOnlyTombstones(now)`), but the fix moved the null guard to the single call site in `CompositesSearcher` and removed the redundant check from each implementation. A `null` return from `getColumnFamily` therefore triggered an NPE in `isStale()` before the fix was applied.

### Before / After

```java
// BEFORE (CompositesIndexOnClusteringKey / CompositesIndexOnPartitionKey)
public boolean isStale(IndexedEntry entry, ColumnFamily data, long now)
{
    return data == null || data.hasOnlyTombstones(now);
}

// BEFORE (CompositesSearcher) — no null check before calling isStale
if (index.isStale(entry, newData, filter.timestamp))
```

```java
// AFTER (CompositesSearcher) — null guard moved here, canonical single location
if (newData == null || index.isStale(entry, newData, filter.timestamp))

// AFTER (isStale implementations) — null check removed; callers own the guard
public boolean isStale(IndexedEntry entry, ColumnFamily data, long now)
{
    return data.hasOnlyTombstones(now);
}
```

---

## How to Spot

When an interface/abstract method accepts an object that may be null, check every implementation and every call site: the null guard belongs at exactly one canonical location. If each implementation duplicates the guard, it is a signal the check should move to the call site (or vice-versa), but inconsistency between them creates latent NPE risk.

---

## Test

A `ColumnFamilyStoreTest` test was added to exercise secondary-index queries against a table that has had rows deleted, covering the path where `getColumnFamily` returns `null`.
