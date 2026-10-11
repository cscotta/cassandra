# Bug 0180: NPE Initializing CFS Before ClusterMetadata Is Available

**Commit**: 8d6d1774e4 · **Ticket**: CASSANDRA-19169
**Subsystem**: db/ColumnFamilyStore, db/compaction/UnifiedCompactionStrategy
**Category**: Missing null / bounds check

---

## Root Cause

`ColumnFamilyStore.localRangesWeighted` called `ClusterMetadata.current()` unconditionally, then passed the result into a condition. During early startup, local-system keyspace `ColumnFamilyStore` instances are initialized before `ClusterMetadata` is populated, causing `ClusterMetadata.current()` to throw an NPE. A related issue in `UnifiedCompactionStrategy.maybeUpdateShardManager` also called `ClusterMetadata.current().epoch` inside a `while` loop without first checking whether a shard-manager refresh was even needed for local keyspaces. The fix short-circuits both call sites by checking `RING_VERSION_IRRELEVANT` (the sentinel used for system keyspace ranges) before accessing `ClusterMetadata`.

### Before / After

```java
// BEFORE – ColumnFamilyStore.localRangesWeighted
ClusterMetadata metadata = ClusterMetadata.current();   // NPE here during early init
if (!SchemaConstants.isLocalSystemKeyspace(getKeyspaceName())
    && getPartitioner() == metadata.partitioner)
```

```java
// AFTER
if (!SchemaConstants.isLocalSystemKeyspace(getKeyspaceName())
    && getPartitioner() == ClusterMetadata.current().partitioner)  // only reached for non-local ks
```

```java
// BEFORE – UnifiedCompactionStrategy: always fetches epoch
while (shardManager == null || shardManager.isOutOfDate(ClusterMetadata.current().epoch.getEpoch()))

// AFTER – short-circuit for local keyspaces
while (shardManager == null ||
       (cfs.localRangesWeighted().ringVersion != ColumnFamilyStore.RING_VERSION_IRRELEVANT
        && shardManager.isOutOfDate(ClusterMetadata.current().epoch.getEpoch())))
```

---

## How to Spot

Any singleton accessed via a static `current()` method that is populated lazily at startup can produce an NPE if invoked from an initialization path that runs before the singleton is ready. Review sites where `ClusterMetadata.current()` (or equivalent) is used without a guard, especially in constructors or methods triggered by schema loading.

---

## Test

No new unit test was added; the fix was validated by the existing distributed test suite for cluster startup.
