# Bug 1496: Fix NPE at compaction leftover cleanup for dropped tables

**Commit**: 254df46b19 · **Ticket**: CASSANDRA-7770
**Subsystem**: service/CassandraDaemon
**Category**: Missing null / bounds check

---

## Root Cause

During startup, `CassandraDaemon` iterates over unfinished compactions recorded in the system keyspace and calls `ColumnFamilyStore.removeUnfinishedCompactionLeftovers` for each entry. The lookup `Schema.instance.getCFMetaData(ks, cf)` returns `null` when the column family has been dropped since the compaction was recorded, but the code passed the result directly to `removeUnfinishedCompactionLeftovers` without a null guard, causing an NPE.

### Before / After

```java
// BEFORE
CFMetaData cfm = Schema.instance.getCFMetaData(kscf.left, kscf.right);
ColumnFamilyStore.removeUnfinishedCompactionLeftovers(cfm, unfinishedCompactions.get(kscf));
```

```java
// AFTER
CFMetaData cfm = Schema.instance.getCFMetaData(kscf.left, kscf.right);
// CFMetaData can be null if CF is already dropped
if (cfm != null)
    ColumnFamilyStore.removeUnfinishedCompactionLeftovers(cfm, unfinishedCompactions.get(kscf));
```

---

## How to Spot

Any call to `Schema.instance.getCFMetaData(...)` can return null for tables that no longer exist; the return value must always be null-checked before use, especially in startup/recovery paths where schema state may lag behind persisted system records.

---

## Test

No test added.
