# Bug 1415: Fix Deletion for Indexes on Primary Key Columns

**Commit**: c345f81e7f · **Ticket**: CASSANDRA-8206
**Subsystem**: src/java/org/apache/cassandra/db/index/
**Category**: Missing override of default method

---

## Root Cause

`SecondaryIndexManager.deleteFromIndex()` (the cleanup path triggered during compaction/cleanup) called the same `delete()` method as the normal write-path tombstone handler. For indexes on primary key columns (`CompositesIndexOnClusteringKey`, `CompositesIndexOnPartitionKey`), it is incorrect to remove the index entry when a single column is updated or deleted because the index key represents the full CQL row, not an individual column. Calling `delete()` during cleanup would spuriously remove valid index entries. The fix splits the contract into two methods — `delete()` for the normal tombstone path (now a deliberate no-op for PK-column indexes) and `deleteForCleanup()` for the compaction/cleanup path — so that each path can be overridden independently.

### Before / After

```java
// BEFORE — cleanup called the same method as the mutation path
((PerColumnSecondaryIndex) index).delete(key.key, column);
```

```java
// AFTER — cleanup has its own dedicated method
((PerColumnSecondaryIndex) index).deleteForCleanup(key.key, column);

// CompositesIndexOnClusteringKey / CompositesIndexOnPartitionKey now override delete():
@Override
public void delete(ByteBuffer rowKey, Column column)
{
    // Do nothing: we don't know if the full row was deleted.
    // Stale entries are cleaned up lazily on read.
}
```

---

## How to Spot

When an abstract base class defines a single method that must serve two semantically distinct call sites (write-path vs. maintenance-path), subclasses cannot specialize each case independently. Look for `abstract void delete()` (or similar) called from both a normal-operation path and a background cleanup path; if those two paths have different correctness requirements, they need separate override points.

---

## Test

No test added.
