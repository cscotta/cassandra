# Bug 0488: CqlInputFormat Regression Using Wrong Primary Range in Multi-DC Setups

**Commit**: a8327eb886 · **Ticket**: CASSANDRA-15637
**Subsystem**: src/java/org/apache/cassandra/hadoop/cql3/CqlInputFormat.java
**Category**: Incorrect filtering / wrong query result

---

## Root Cause

When `system.size_estimates` was replaced by `system.table_estimates`, the new table added a `range_type` column distinguishing `primary` from `local_primary` ranges. The Hadoop `CqlInputFormat` continued to query using the older API that returned all primary ranges rather than the DC-local primary ranges, causing a multi-DC cluster to generate splits derived from other DCs' tokens. Additionally, wrapped token ranges (crossing the min/max token boundary) were not unwrapped before being submitted as split tasks, which produced invalid range queries and silently incorrect splits.

### Before / After

```java
// BEFORE
Map<TokenRange, Set<Host>> masterRangeNodes = getRangeMap(keyspace, metadata);
for (TokenRange range : masterRangeNodes.keySet()) {
    splitfutures.add(executor.submit(new SplitCallable(range, ...)));
}
```

```java
// AFTER
Map<TokenRange, List<Host>> masterRangeNodes = getRangeMap(keyspace, metadata, getTargetDC(metadata, inputInitialAddress));
for (TokenRange range : masterRangeNodes.keySet()) {
    for (TokenRange unwrapped : range.unwrap()) {
        SplitFuture task = new SplitFuture(new SplitCallable(unwrapped, ...));
        executor.submit(task);
        splitfutures.add(task);
    }
}
```

---

## How to Spot

When a table is migrated from one key/column schema to another and the consumer is updated to use the new table, verify that any DC-affinity or topology-aware semantics provided implicitly by the old API are explicitly reproduced in the new one. In multi-DC Cassandra, estimates are stored per node; if a Hadoop job selects ranges from the wrong DC, queries will silently go to the wrong replicas with stale or absent size estimates.

---

## Test

`StorageServiceServerTest` checks that `table_estimates` is populated with both `primary` and `local_primary` range types and that their row counts are correct per-keyspace.
