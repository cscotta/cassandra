# Bug 0331: Race Condition in AbstractReplicationStrategy Endpoint Cache

**Commit**: b1a8a56c56 · **Ticket**: CASSANDRA-16673
**Subsystem**: src/java/org/apache/cassandra/locator
**Category**: Race condition / missing synchronization

---

## Root Cause

`AbstractReplicationStrategy` cached computed natural endpoints in a `NonBlockingHashMap<Token, ArrayList<InetAddress>>` and used a `volatile long lastInvalidatedVersion` field to detect when the ring changed. Invalidation used a double-checked lock: if `lastVersion > lastInvalidatedVersion`, acquire `this`, re-check, then clear the map and update the version. The flaw is that after the cache was cleared but before the new ring-version was stored, another thread could read `lastInvalidatedVersion` as still equal to the old (pre-clear) version, find its key absent from the now-empty map, compute endpoints using a stale `TokenMetadata` snapshot, and insert stale endpoints. A separate defect in `TokenMetadata.cloneOnlyTokenMap()` failed to propagate `ringVersion` into the cloned instance, so `tm.getRingVersion()` always returned 0 on the clone, making cache entries keyed to version 0 appear valid regardless of ring changes.

### Before / After

```java
// BEFORE – clear then update; window where version is stale
synchronized (this) {
    if (lastVersion > lastInvalidatedVersion) {
        cachedEndpoints.clear();
        lastInvalidatedVersion = lastVersion; // <-- written after clear
    }
}
return cachedEndpoints.get(t); // another thread may race here
```

```java
// AFTER – AtomicReference swap: stale holder is garbage-collected,
// ring version is embedded in the holder itself
ReplicaHolder<K,V> cleaned = new ReplicaHolder<>(ringVersion, current.replicas.size());
cachedReplicas.compareAndSet(current, cleaned);
// re-read; if a newer version won the CAS, return null to force recalc
```

```java
// BEFORE – clone lost ringVersion
return new TokenMetadata(tokenToEndpointMap, endpointToHostIdMap, topology, partitioner);

// AFTER – clone preserves ringVersion
return new TokenMetadata(tokenToEndpointMap, endpointToHostIdMap, topology, partitioner, ringVersion);
```

---

## How to Spot

Double-checked locking that clears a shared data structure before updating the version guard leaves a window where the invariant "if version matches, data is valid" is broken. The safe pattern is to replace the entire holder atomically (e.g., via `AtomicReference.compareAndSet`) so that the version and the data it guards are always in sync. Always verify that clone/copy constructors propagate all fields used as cache keys or version markers.

---

## Test

`ReadsDuringBootstrapTest.readsDuringBootstrapTest` uses ByteBuddy to pause endpoint lookups on node 1 while a fourth node bootstraps, then verifies that reads complete correctly after the ring change settles.
