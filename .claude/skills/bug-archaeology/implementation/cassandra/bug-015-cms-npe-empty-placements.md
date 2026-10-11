# Bug 015: NPE in StorageService When Meta Keyspace Placements Are Empty

**Commit**: ec7794f20fc0cd07a37901a43f07f4e65517cb08 · **Ticket**: CASSANDRA-21004
**Subsystem**: StorageService / CMS (Cluster Metadata Service)
**Category**: Missing null / bounds check

---

## Root Cause

`StorageService.getRangeToAddressMap` unconditionally called `placement.reads.forRange(...).get()` for the `system_cluster_metadata` (meta) keyspace without first checking whether the placement had any read endpoints. During a rolling upgrade, before the CMS is initialized on the upgraded node, the meta keyspace placement exists in `ClusterMetadata` but its read set is empty. Calling `forRange(...).get()` on an empty placement returned `null`, which was immediately inserted into the range-to-endpoint map, causing a `NullPointerException` in downstream consumers. The fix adds a guard that skips the map insertion when the read placement is empty.

### Before / After

```java
// BEFORE — unconditional get() on potentially empty placement
if (keyspaceMetadata.params.replication.isMeta())
{
    rangeToEndpointMap.put(MetaStrategy.entireRange,
                           metadata.placements.get(keyspaceMetadata.params.replication)
                           .reads.forRange(MetaStrategy.entireRange).get());
}

// AFTER — guard against empty placement mid-upgrade
if (keyspaceMetadata.params.replication.isMeta())
{
    DataPlacement placement = metadata.placements.get(keyspaceMetadata.params.replication);
    // May be empty if mid-upgrade and CMS is not yet initialized
    if (!placement.reads.isEmpty())
        rangeToEndpointMap.put(MetaStrategy.entireRange,
                               placement.reads.forRange(MetaStrategy.entireRange).get());
}
```

---

## How to Spot

Any code that looks up a value from a collection and calls `.get()` without first verifying that the collection is non-empty (or that the key is present) is a candidate for this class of bug. The risk is amplified in upgrade paths where invariants that hold in steady state may be temporarily violated — in particular, placements or topology metadata that is "always populated" in a fully initialized cluster can be empty during bootstrap or upgrade transitions. Reviewers should ask: "Can this data structure ever be empty or absent, even transiently?" and ensure a guard is present whenever the answer is yes.

---

## Test

`ClusterMetadataSingleNodeUpgradeTest.runAfterClusterUpgrade` now asserts that `getRangeToAddressMap("system_cluster_metadata")` returns an empty map (rather than throwing an NPE) immediately after upgrade but before `cms initialize` is called.
