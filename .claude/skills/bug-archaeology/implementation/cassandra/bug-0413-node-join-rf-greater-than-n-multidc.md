# Bug 0413: Node Fails to Bootstrap When RF > Number of Nodes in a Datacenter (Multi-DC)

**Commit**: 87e4f31e30 · **Ticket**: CASSANDRA-16296
**Subsystem**: src/java/org/apache/cassandra/dht/RangeStreamer.java, src/java/org/apache/cassandra/locator/NetworkTopologyStrategy.java
**Category**: Logic error in condition

---

## Root Cause

`RangeStreamer.useStrictSourcesForRanges` determined whether to require strict source selection by comparing the total cluster size against the strategy's aggregate RF: `metadata.getSizeOfAllEndpoints() != strat.getReplicationFactor().allReplicas`. In a multi-DC cluster this comparison used the cluster-wide node count against the sum of all DCs' RF, meaning that if only one DC had RF > its node count the check could still pass (or fail) incorrectly. A node joining that DC would then fail streaming because it tried to find replicas for ranges that did not yet have a full set of sources. The fix computes per-DC comparisons for `NetworkTopologyStrategy`, counting only nodes belonging to DCs that actually have a positive RF.

### Before / After

```java
// BEFORE — single cluster-wide comparison
private boolean useStrictSourcesForRanges(AbstractReplicationStrategy strat) {
    return useStrictConsistency
            && tokens != null
            && metadata.getSizeOfAllEndpoints() != strat.getReplicationFactor().allReplicas;
}
```

```java
// AFTER — per-DC comparison for NTS
private boolean useStrictSourcesForRanges(AbstractReplicationStrategy strat) {
    boolean res = useStrictConsistency && tokens != null;
    if (res) {
        int nodes = 0;
        if (strat instanceof NetworkTopologyStrategy) {
            ImmutableMultimap<String, InetAddressAndPort> dc2Nodes = metadata.getDC2AllEndpoints(snitch);
            for (String dc : dc2Nodes.keySet())
                nodes += ntps.getReplicationFactor(dc).allReplicas > 0 ? dc2Nodes.get(dc).size() : 0;
        } else {
            nodes = metadata.getSizeOfAllEndpoints();
        }
        res = nodes > strat.getReplicationFactor().allReplicas;
    }
    return res;
}
```

---

## How to Spot

Any topology check that aggregates node counts across all datacenters and then compares against an aggregate RF should be scrutinised for multi-DC clusters: the global sum can mask DC-level imbalances. Prefer per-DC comparisons when using `NetworkTopologyStrategy`.

---

## Test

`AlterTest.testCreateAlterKeyspacesRFWarnings` verifies that a client warning is emitted when RF exceeds node count; integration tests for bootstrap in RF > N scenarios were added in `AlterTest` and `CreateTest`.
