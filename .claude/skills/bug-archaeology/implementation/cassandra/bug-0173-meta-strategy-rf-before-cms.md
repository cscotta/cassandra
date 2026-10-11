# Bug 0173: NPE Getting Replication Factor for system_cluster_metadata Before CMS Is Available

**Commit**: 43eb2a4bde · **Ticket**: CASSANDRA-19269
**Subsystem**: locator/MetaStrategy
**Category**: Missing null / bounds check

---

## Root Cause

`MetaStrategy.getReplicationFactor()` called `ClusterMetadata.current()` which throws or returns a non-null object only once the Cluster Metadata Service (CMS) has been initialized. During early startup — before the CMS has bootstrapped but after the schema is loaded — some code paths query the replication factor for `system_cluster_metadata`. This caused a crash because `metadata.placements.get(ReplicationParams.meta(metadata))` would either throw a `NullPointerException` or attempt to dereference an uninitialized placement map. The fix uses `ClusterMetadata.currentNullable()`, returning a safe default RF of 1 when metadata is null or still at the very first synthetic epoch (`Epoch.FIRST`).

### Before / After

```java
// BEFORE
ClusterMetadata metadata = ClusterMetadata.current();
int rf = metadata.placements.get(ReplicationParams.meta(metadata)).writes.forRange(entireRange).get().byEndpoint.size();
return ReplicationFactor.fullOnly(rf);
```

```java
// AFTER
ClusterMetadata metadata = ClusterMetadata.currentNullable();
if (metadata == null || metadata.epoch.isEqualOrBefore(Epoch.FIRST))
    return ReplicationFactor.fullOnly(1);
int rf = metadata.placements.get(ReplicationParams.meta(metadata)).writes.forRange(entireRange).get().byEndpoint.size();
return ReplicationFactor.fullOnly(rf);
```

---

## How to Spot

Any code that reads from `ClusterMetadata.current()` and is reachable before full CMS initialization (e.g., from schema loading, snitch resolution, or storage service startup) must guard against a null or pre-initialized metadata. The pattern `ClusterMetadata.current()` (as opposed to `currentNullable()`) is only safe after the CMS startup sequence completes; audit all call sites in code paths that execute before `StorageService.initServer()` finishes.

---

## Test

No new test added; the fix was verified by observing that startup no longer throws during the replication-factor lookup on the metadata keyspace.
