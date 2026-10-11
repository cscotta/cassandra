# Bug 0111: RFP Applies Row Limit When Fetching Partition With Unresolved Static Row

**Commit**: 54831659 · **Ticket**: CASSANDRA-20323
**Subsystem**: service/reads (ReplicaFilteringProtection)
**Category**: Incorrect filtering / wrong query result

---

## Root Cause

`ReplicaFilteringProtection` issues follow-up reads to silent replicas for rows that may be stale. When a static row is unresolved (i.e., some replica is silent about it), the code set `limits = DataLimits.cqlLimits(1)` — a limit of one row — before issuing the per-partition fetch. A static row predicate can match any regular row in the partition, so limiting the fetch to one row means the coordinator may miss live rows that exist further into the partition on the silent replica, ultimately returning an incomplete result set to the client. Additionally, the column divergence loop iterated over all `columns` (including statics) even for regular rows, causing static columns to be checked against regular row content. The fix unconditionally uses `DataLimits.NONE` for the follow-up read, and changes the column divergence loop to iterate only `columns.statics` for static rows and `columns.regulars` for regular rows.

### Before / After

```java
// BEFORE — limits follow-up fetch to 1 row when only a static is needed
DataLimits limits = clusterings.isEmpty() ? DataLimits.cqlLimits(1) : DataLimits.NONE;
ClusteringIndexFilter filter = unresolvedStatic ? command.clusteringIndexFilter(key)
                                                : new ClusteringIndexNamesFilter(clusterings, ...);
SinglePartitionReadCommand cmd = SinglePartitionReadCommand.create(..., limits, key, filter);
```

```java
// AFTER — no limit regardless of unresolved static
ClusteringIndexFilter filter = unresolvedStatic ? command.clusteringIndexFilter(key)
                                                : new ClusteringIndexNamesFilter(clusterings, ...);
SinglePartitionReadCommand cmd = SinglePartitionReadCommand.create(..., DataLimits.NONE, key, filter);
```

---

## How to Spot

When issuing compensating reads for replica filtering protection, any limit must be justified by a proof that the limit cannot suppress valid matches. A static column predicate matches all regular rows in a partition, so a per-row limit is unsafe. Review all `DataLimits.cqlLimits(N)` constructions inside `ReplicaFilteringProtection` and confirm that the limit is not applied when the read covers an entire partition due to unresolved static content.

---

## Test

`ReplicaFilteringWithStaticsTest.testStaticMatchWithPartitionDelete` sets up a partition where node 2 holds an update to the static column and node 3 has a stale row not present on other replicas; with paging size 1, it verifies all matching rows (`A`, `C`, `F`, `G`) are returned rather than a truncated set.
