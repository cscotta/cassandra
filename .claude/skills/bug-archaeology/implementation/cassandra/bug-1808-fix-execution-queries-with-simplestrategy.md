# Bug 1808: Fix execution of LOCAL_QUORUM queries with SimpleStrategy

**Commit**: 32d7cb5066 · **Ticket**: CASSANDRA-6545
**Subsystem**: src/java/org/apache/cassandra/db/ConsistencyLevel.java
**Category**: Logic error in condition

---

## Root Cause

`LOCAL_QUORUM` and `EACH_QUORUM` consistency levels hardcoded an unconditional cast to `NetworkTopologyStrategy`, which threw a `ClassCastException` at runtime when the keyspace used `SimpleStrategy`. The fix adds an `instanceof` guard so that when `SimpleStrategy` is in use, both consistency levels fall back to a plain quorum calculation (RF/2 + 1) instead of attempting per-datacenter logic that `SimpleStrategy` cannot provide.

### Before / After

```java
// BEFORE
private int localQuorumFor(Table table, String dc)
{
    return (((NetworkTopologyStrategy) table.getReplicationStrategy()).getReplicationFactor(dc) / 2) + 1;
}
```

```java
// AFTER
private int localQuorumFor(Table table, String dc)
{
    return (table.getReplicationStrategy() instanceof NetworkTopologyStrategy)
         ? (((NetworkTopologyStrategy) table.getReplicationStrategy()).getReplicationFactor(dc) / 2) + 1
         : quorumFor(table);
}
```

---

## How to Spot

Whenever a method unconditionally casts an interface to a concrete subtype, ask whether other subtypes exist and what happens when they are used. A missing `instanceof` guard before a downcast is a common source of runtime exceptions that static analysis tools or a simple code review can surface.

---

## Test

No test added for the fix itself; the change was validated via the CASSANDRA-6545 ticket. The scenario is: issue a `LOCAL_QUORUM` (or `EACH_QUORUM`) write/read against a keyspace that uses `SimpleStrategy` — before the fix this throws `ClassCastException`; after it succeeds with quorum semantics.
