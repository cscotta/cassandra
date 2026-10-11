# Bug 0391: False UnavailableException Due to Cluster Topology Changes Mid-Query

**Commit**: b915688ea8 · **Ticket**: CASSANDRA-16545
**Subsystem**: src/java/org/apache/cassandra/locator, src/java/org/apache/cassandra/db/ConsistencyLevel.java
**Category**: Race condition / missing synchronization

---

## Root Cause

`ConsistencyLevel.blockFor()` and all the quorum-calculation helpers (`quorumFor`, `localQuorumFor`, `eachQuorumForRead`, etc.) accepted a `Keyspace` object and called `keyspace.getReplicationStrategy()` at the moment `blockFor` was evaluated. If the replication strategy changed between when the replica plan was assembled (e.g., during a topology change, `ALTER KEYSPACE`, or node add/remove) and when `blockFor` was evaluated to check whether enough replicas responded, the two calls could observe different replication factors. A live-node set built with RF=3 could then be compared against a `blockFor` value computed with RF=5, making it appear that insufficient replicas responded and raising a spurious `UnavailableException`. The fix snapshots the `AbstractReplicationStrategy` at the time the `ReplicaLayout` / `ReplicaPlan` is constructed and passes that single snapshot all the way through to `blockFor`, so all consistency calculations for a given query use a consistent view of topology.

### Before / After

```java
// BEFORE – blockFor() fetches the strategy on the fly
public int blockFor(Keyspace keyspace)
{
    switch (this)
    {
        case QUORUM: return quorumFor(keyspace);  // keyspace.getReplicationStrategy() called here
        ...
    }
}
```

```java
// AFTER – blockFor() operates on the snapshot taken at plan creation
public int blockFor(AbstractReplicationStrategy replicationStrategy)
{
    switch (this)
    {
        case QUORUM: return quorumFor(replicationStrategy);  // snapshot, not live lookup
        ...
    }
}
// ReplicaLayout and ReplicaPlan now carry the AbstractReplicationStrategy snapshot
```

---

## How to Spot

Whenever a consistency check comparing "live responses received" vs. "responses required" reads cluster metadata (replication factor, DC membership) from a live object rather than from a value captured at the start of the request, a concurrent topology change can make the two sides disagree. Review any `blockFor`, `quorumFor`, or `requiredFor` helper that calls `Keyspace.open()` or `keyspace.getReplicationStrategy()` inside the check: those should instead use a snapshot taken when the replica set was resolved.

---

## Test

`AssureSufficientLiveNodesTest` in `test/unit/org/apache/cassandra/locator/` was added with 303 lines of new test coverage asserting that the correct `UnavailableException` (or no exception) is raised for various combinations of live/pending/down replicas and consistency levels.
