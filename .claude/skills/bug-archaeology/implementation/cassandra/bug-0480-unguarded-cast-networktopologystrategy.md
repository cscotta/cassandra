# Bug 0480: Unguarded Cast to NetworkTopologyStrategy in EACH_QUORUM Handlers

**Commit**: a209e78dae · **Ticket**: CASSANDRA-15791
**Subsystem**: `src/java/org/apache/cassandra/db/ConsistencyLevel.java`, `src/java/org/apache/cassandra/service/DatacenterSyncWriteResponseHandler.java`
**Category**: Missing null / bounds check

---

## Root Cause

Two methods that handle `EACH_QUORUM` consistency — `ConsistencyLevel.eachQuorumForRead()` and `DatacenterSyncWriteResponseHandler` constructor — unconditionally cast the keyspace replication strategy to `NetworkTopologyStrategy`. When a keyspace uses `SimpleStrategy` (or any non-NTS strategy), `EACH_QUORUM` is technically valid, but the hard cast throws a `ClassCastException`. The code assumed `EACH_QUORUM` implies NTS without verifying the actual strategy type.

### Before / After

```java
// BEFORE
NetworkTopologyStrategy strategy =
    (NetworkTopologyStrategy) keyspace.getReplicationStrategy(); // ClassCastException on SimpleStrategy
for (String dc : strategy.getDatacenters())
    perDc.put(dc, ConsistencyLevel.localQuorumFor(keyspace, dc));
return perDc;
```

```java
// AFTER
AbstractReplicationStrategy strategy = keyspace.getReplicationStrategy();
if (strategy instanceof NetworkTopologyStrategy) {
    NetworkTopologyStrategy npStrategy = (NetworkTopologyStrategy) strategy;
    // ... NTS path using getDatacenters()
} else {
    ObjectIntHashMap<String> perDc = new ObjectIntHashMap<>(1);
    perDc.put(DatabaseDescriptor.getLocalDataCenter(), quorumFor(keyspace));
    return perDc;
}
```

---

## How to Spot

Wherever a replication-strategy object is retrieved and immediately cast to a concrete subtype without an `instanceof` guard, check whether all callers enforce the precondition upstream. The `EACH_QUORUM` consistency level is valid for keyspaces that use `SimpleStrategy`, so any code path that reaches a NTS-specific operation via CL alone needs a type check. Search for `(NetworkTopologyStrategy)` casts in CL or write-handler code to find similar unguarded patterns.

---

## Test

No test added.
