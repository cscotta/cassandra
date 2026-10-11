# Bug 0891: NPE in CompactionMetrics When CompactionInfo Has No Table

**Commit**: 4754cddfb9 · **Ticket**: CASSANDRA-11961
**Subsystem**: Compaction / Metrics
**Category**: Missing null / bounds check

---

## Root Cause
`CompactionMetrics` iterates over all in-progress compactions to build a per-keyspace/table pending-task count map. For certain compaction types (such as index or validation compactions) `CompactionInfo.getCFMetaData()` legitimately returns `null` because no specific table is associated with the task. The code accessed `metaData.ksName` without a null guard, producing a `NullPointerException` that, while not fatal to the node (it was caught upstream), polluted logs and prevented the metric from being accurately computed whenever such a compaction was running.

### Before / After
```java
// BEFORE — no null check; NPE when metaData is null
CFMetaData metaData = compaction.getCompactionInfo().getCFMetaData();
if (!resultMap.containsKey(metaData.ksName)) { ... }

// AFTER — skip compactions with no associated table
CFMetaData metaData = compaction.getCompactionInfo().getCFMetaData();
if (metaData == null) {
    continue;
}
if (!resultMap.containsKey(metaData.ksName)) { ... }
```

---

## How to Spot
Any code that accesses fields of an object returned by a method documented to return `null` for certain subtypes (such as compaction holders that span index builds, anticompaction, or validation) must guard against null before dereferencing. The tell is when a method's javadoc or usages in other paths already handle null — here other callers of `getCFMetaData()` already performed null checks, making the absence in the metrics path an oversight.

---

## Test
No test added.
