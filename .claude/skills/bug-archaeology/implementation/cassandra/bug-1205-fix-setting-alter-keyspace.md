# Bug 1205: ALTER KEYSPACE durable_writes Change Not Visible After Update

**Commit**: 3504a50bb4 · **Ticket**: CASSANDRA-9560
**Subsystem**: src/java/org/apache/cassandra/db/Keyspace.java
**Category**: State not cleaned up

---

## Root Cause

`Keyspace.metadata` was declared `public final`, so it was set once at construction and never updated. When `ALTER KEYSPACE ... WITH durable_writes = false` was executed, `DefsTables.updateKeyspace()` called `Keyspace.open(ksName).createReplicationStrategy(newKsm)` which updated the replication strategy but left `metadata` pointing at the old `KSMetaData` object. All subsequent reads of `keyspace.metadata.durableWrites` — in `RowMutation.apply()`, `ColumnFamilyStore.truncate()`, and `DataTracker.renewMemtable()` — continued to see the pre-`ALTER` value. The fix makes `metadata` a `volatile` field, introduces a `setMetadata(KSMetaData)` method that updates both the field and the replication strategy atomically (from the caller's perspective), and replaces direct field access with `getMetadata()`.

### Before / After

```java
// BEFORE – final field never updated after ALTER KEYSPACE
public final KSMetaData metadata;

// DefsTables.updateKeyspace():
Keyspace.open(ksName).createReplicationStrategy(newKsm); // metadata untouched
```

```java
// AFTER – volatile field updated atomically with replication strategy
private volatile KSMetaData metadata;

public void setMetadata(KSMetaData ksm) {
    this.metadata = ksm;
    createReplicationStrategy(ksm);
}

// DefsTables.updateKeyspace():
Keyspace.open(ksName).setMetadata(newKsm);
```

---

## How to Spot

Any mutable cluster-configuration value stored in a `final` field on a long-lived singleton is a red flag: DDL operations that update that configuration will silently have no effect. Code review should confirm that every DDL handler (alter table, alter keyspace, etc.) updates all derived state in the live object, not just the schema store. Volatile visibility is also required whenever worker threads read the field without holding a lock.

---

## Test

`CommitLogTest` updated to use `getMetadata()` instead of direct field access. No new test for the ALTER path added.
