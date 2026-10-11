# Bug 0081: Accord Topology Replay Incorrect During Bootstrap and Startup

**Commit**: dfcf3aff4e · **Ticket**: CASSANDRA-20142
**Subsystem**: service/accord/AccordConfigurationService, tcm/membership/Directory
**Category**: State not cleaned up

---

## Root Cause

During restart, Accord relied on `ClusterMetadata#current` to fetch historical epochs; because there is no guarantee of seeing consecutive epochs via that path, topology replay could silently skip epochs or use stale data. Additionally, `redundant`/`closed` ranges were set with incorrect ranges in one code path during replay, and removed-node information was discovered by re-reading historical TCM epochs instead of being taken from the authoritative `Directory` — causing repeated, expensive TCM round-trips and leaving the node waiting on removed peers.

### Before / After

```java
// BEFORE – topology loaded directly from disk state, including full topology objects;
// removed nodes discovered per-epoch by diffing topology.nodes()
diskState = diskStateManager.loadTopologies(((epoch, metadata, topology, ...) -> {
    ...
    Sets.SetView<Node.Id> removedNodes = Sets.difference(previous.nodes(), topology.nodes());
    if (!removedNodes.isEmpty())
        onNodesRemoved(topology.epoch(), currentTopology(), removedNodes);
    ...
}));
```

```java
// AFTER – topology objects loaded from journal; only sync/closed/redundant metadata
// loaded from disk state; removed nodes read once from Directory
diskStateManager.loadLocalTopologyState((epoch, syncStatus, ...) -> {
    ...
});
Map<Node.Id, Long> removedNodes = mapping.removedNodes();
for (Map.Entry<Node.Id, Long> e : removedNodes.entrySet())
    onNodeRemoved(e.getValue(), currentTopology(), e.getKey());
```

---

## How to Spot

When a system reconstructs topology from two overlapping sources — a journal and a metadata table — verify there is a single authoritative source for each piece of information and that none of the sources can lag. Code that computes removed nodes by diffing sequential snapshots is fragile: any skipped snapshot loses the removal event. Prefer explicit removal records in a stable store.

---

## Test

A new `AccordBootstrapTest` (distributed fuzz test) exercises bootstrap + topology replay with Accord enabled.
