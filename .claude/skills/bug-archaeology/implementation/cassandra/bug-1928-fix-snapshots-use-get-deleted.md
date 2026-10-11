# Bug 1928: Snapshots In Use Get Deleted During Snapshot Repair

**Commit**: 806a45225b · **Ticket**: CASSANDRA-6011
**Subsystem**: src/java/org/apache/cassandra/db/compaction/CompactionManager.java
**Category**: Logic error in condition

---

## Root Cause

During snapshot repair, `CompactionManager` called `cfs.table.snapshotExists()` and `cfs.table.clearSnapshot()` — methods on the `Table` object — to check and clear the repair snapshot. However, these `Table`-level methods check across all column families, meaning snapshots belonging to other column families with the same session ID could match. The correct check is `cfs.snapshotExists()` and `cfs.clearSnapshot()`, which are scoped to the specific `ColumnFamilyStore` being repaired, avoiding unintended deletion of snapshots from other column families that are still in use.

### Before / After

```java
// BEFORE
if (cfs.table.snapshotExists(validator.request.sessionid))
    cfs.table.clearSnapshot(validator.request.sessionid);
```

```java
// AFTER
if (cfs.snapshotExists(validator.request.sessionid))
    cfs.clearSnapshot(validator.request.sessionid);
```

---

## How to Spot

When snapshot lifecycle methods are called, verify the scope: `Table`-level snapshot operations affect all column families in the keyspace, while `ColumnFamilyStore`-level operations are scoped to a single table. Calling `table.clearSnapshot()` during per-CFS repair can wipe snapshots from other column families sharing the same session ID.

---

## Test

No test added.
