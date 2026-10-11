# Bug 1350: Fix sstableupgrade throws exception

**Commit**: ffd0ae4bf9 · **Ticket**: CASSANDRA-8688
**Subsystem**: src/java/org/apache/cassandra/db/compaction/Upgrader.java
**Category**: Logic error in condition

---

## Root Cause

The `Upgrader` class, used by the `sstableupgrade` tool, called `writer.finish()` and then passed the returned list of new `SSTableReader` instances to `cfs.getDataTracker().markCompactedSSTablesReplaced(...)`. However, during an offline upgrade the `ColumnFamilyStore` and its data tracker are not in a fully initialized state, so calling `markCompactedSSTablesReplaced` threw an exception. The tracking call is only needed when upgrading in a live cluster; for the offline tool it is unnecessary because the upgraded SSTables simply replace the originals on disk. Dropping the call to `markCompactedSSTablesReplaced` (and the now-unused return value from `writer.finish()`) fixes the exception without affecting correctness.

### Before / After

```java
// BEFORE
List<SSTableReader> sstables = writer.finish();
cfs.getDataTracker().markCompactedSSTablesReplaced(toUpgrade, sstables, OperationType.UPGRADE_SSTABLES);
outputHandler.output("Upgrade of " + sstable + " complete.");
```

```java
// AFTER
writer.finish();
outputHandler.output("Upgrade of " + sstable + " complete.");
```

---

## How to Spot

When offline maintenance tools reuse classes designed for a live cluster, watch for calls that assume the cluster is running (data trackers, gossip state, schema registry). Any method that mutates in-memory cluster state is suspect in an offline tool. Review whether the return value of such methods is used, and whether the operation it triggers is even meaningful in the offline context.

---

## Test

No test added.
