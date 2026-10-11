# Bug 0619: Disk Boundaries Invalidated on Every Replication Strategy Instance Creation

**Commit**: 9337ca316a · **Ticket**: CASSANDRA-14083
**Subsystem**: `db/Keyspace.java`, `db/DiskBoundaryManager.java`
**Category**: State not cleaned up

---

## Root Cause

`Keyspace.setMetadata()` called `columnFamilyStores.values().forEach(ColumnFamilyStore::invalidateDiskBoundaries)` unconditionally every time a new `AbstractReplicationStrategy` instance was constructed, even if the replication parameters had not actually changed. Because schema gossip can trigger repeated `setMetadata` calls with the same settings, disk boundaries were flushed needlessly, forcing expensive recomputation of per-disk token ranges. An additional fix in `DiskBoundaryManager` ensures that when the node is bootstrapping, `PendingRangeCalculatorService` is allowed to finish before reading pending ranges, preventing a race that could also cause spurious invalidation.

### Before / After

```java
// BEFORE (Keyspace.java)
// always invalidates on any setMetadata call
logger.debug("New replication strategy instance - invalidating disk boundary cache");
columnFamilyStores.values().forEach(ColumnFamilyStore::invalidateDiskBoundaries);
```

```java
// AFTER (Keyspace.java)
if (!ksm.params.replication.equals(replicationParams))
{
    logger.debug("New replication settings for keyspace {} - invalidating disk boundary caches", ksm.name);
    columnFamilyStores.values().forEach(ColumnFamilyStore::invalidateDiskBoundaries);
}
replicationParams = ksm.params.replication;
```

---

## How to Spot

Any cache or derived-state invalidation that runs without first comparing old and new values is a candidate for this pattern. When reviewing schema-update paths, check whether the invalidation is gated on an actual change. The absence of a stored "previous params" field is a structural clue that equality is never being tested.

---

## Test

`DiskBoundaryManagerTest.alterKeyspaceTest` verifies that running `ALTER KEYSPACE` with changed replication produces a new `DiskBoundaries` instance, and that a second call with unchanged settings reuses the same cached instance.
