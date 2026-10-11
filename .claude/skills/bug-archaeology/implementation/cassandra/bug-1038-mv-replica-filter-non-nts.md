# Bug 1038: MV Natural Endpoint Calculation Ignores Non-NTS Replicas

**Commit**: 1bd2c94252 · **Ticket**: CASSANDRA-10634
**Subsystem**: Materialized Views / Replication
**Category**: Incorrect filtering / wrong query result

---

## Root Cause
`ViewUtils.getViewNaturalEndpoint` calculates which replica is responsible for applying a given MV update by matching cardinality positions between the base-token replica list and the view-token replica list. The code was written to filter both lists to the local datacenter before comparing them — which is correct only for `NetworkTopologyStrategy`. With `SimpleStrategy` (or any non-NTS strategy), the datacenter filter silently dropped all remote replicas from the lists, producing shorter-than-expected endpoint arrays. The subsequent `assert baseEndpoints.size() == viewEndpoints.size()` would then fail, and even when it did not, `indexOf(localAddress)` could return -1 causing the method to return `null` and skip the MV update entirely.

The fix wraps every datacenter membership check with `instanceof NetworkTopologyStrategy`: when the strategy is not NTS, every replica is treated as local and no filtering is applied.

### Before / After
```java
// BEFORE – always filters to local datacenter, wrong for SimpleStrategy
for (InetAddress baseEndpoint : replicationStrategy.getNaturalEndpoints(baseToken))
{
    if (DatabaseDescriptor.getEndpointSnitch().getDatacenter(baseEndpoint).equals(localDataCenter))
        localBaseEndpoints.add(baseEndpoint);
}

// AFTER – filter only when using NetworkTopologyStrategy
for (InetAddress baseEndpoint : replicationStrategy.getNaturalEndpoints(baseToken))
{
    if (!(replicationStrategy instanceof NetworkTopologyStrategy) ||
        DatabaseDescriptor.getEndpointSnitch().getDatacenter(baseEndpoint).equals(localDataCenter))
        baseEndpoints.add(baseEndpoint);
}
```

---

## How to Spot
Any replication utility that conditions behavior on datacenter membership without first checking whether the strategy is topology-aware is vulnerable to this pattern. In review, ensure that datacenter-level filtering is always guarded by `instanceof NetworkTopologyStrategy` (or equivalent), and that assertion-level invariants like equal list sizes are tested against keyspaces using `SimpleStrategy`, not just `NetworkTopologyStrategy` clusters.

---

## Test
No test added.
