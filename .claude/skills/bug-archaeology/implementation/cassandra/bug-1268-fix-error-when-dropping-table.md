# Bug 1268: Fix Error When Dropping Table During Compaction

**Commit**: 369966a2af · **Ticket**: CASSANDRA-9251
**Subsystem**: src/java/org/apache/cassandra/db/compaction/CompactionManager.java, db/ColumnFamilyStore.java, db/DataTracker.java, db/Keyspace.java
**Category**: Race condition / missing synchronization

---

## Root Cause

When a table was dropped while a compaction was in progress, the compaction strategy was shut down and SSTable references were released inside `ColumnFamilyStore.invalidate()` without first waiting for in-flight compactions to stop. The compaction thread could then race with the drop: it would call `DataTracker.unmarkCompacting()` after the SSTables had already been released, encounter the now-invalid view, and attempt to mark them obsolete or dereference them again, triggering errors or double-frees. Additionally, the old `DataTracker.unmarkCompacting()` logic snapshotted `isValid()` before the CAS loop and acted on that stale value, so a concurrent `invalidate()` could slip in between the snapshot and the CAS, again producing incorrect behaviour.

### Before / After

```java
// BEFORE (Keyspace.dropCf): compaction strategy not shut down before barriers
cfs.keyspace.writeOrder.awaitNewBarrier();

// BEFORE (DataTracker.unmarkCompacting): stale isValid snapshot
boolean isValid = cfstore.isValid();
...
if (!isValid) { for (SSTableReader s : unmark) s.markObsolete(); }
```

```java
// AFTER (Keyspace.dropCf): strategy shut down and compactions interrupted first
cfs.getCompactionStrategy().shutdown();
CompactionManager.instance.interruptCompactionForCFs(cfs.concatWithIndexes(), true);
cfs.keyspace.writeOrder.awaitNewBarrier();

// AFTER (DataTracker.unmarkCompacting): live check after CAS
if (!cfstore.isValid()) { ... unreference ... }
```

---

## How to Spot

When tearing down a stateful subsystem (table drop, column family invalidation), check every concurrent activity that holds a reference to that subsystem. If the teardown does not interrupt-and-wait before releasing shared state, any concurrent activity that completes after release will observe freed or inconsistent state. Look for `shutdown()` or `unreference()` calls that are not preceded by a corresponding `interrupt + await` pair.

---

## Test

`CrcCheckChanceTest.testDropDuringCompaction` — starts a large compaction throttled to 1 MB/s, issues `DROP TABLE` concurrently, and waits for the compaction future to complete without error.
