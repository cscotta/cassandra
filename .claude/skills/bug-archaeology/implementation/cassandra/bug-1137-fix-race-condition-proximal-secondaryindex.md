# Bug 1137: Race Condition on Proximal SecondaryIndex Drop/Create

**Commit**: 45c04b7275 · **Ticket**: CASSANDRA-9908
**Subsystem**: src/java/org/apache/cassandra/db/index/AbstractSimplePerColumnSecondaryIndex.java
**Category**: Race condition / missing synchronization

---

## Root Cause

When a secondary index was dropped and immediately recreated, the old index's SSTables and transaction log files could still be in the process of asynchronous deletion when the new index was being initialized. The `removeIndex` method called `indexCfs.invalidate()` without first draining in-flight compactions, flushing pending writes, or waiting for all pending deletions to complete. This meant the new index could see stale files from the previous incarnation and treat them as its own, corrupting the index state.

### Before / After

```java
// BEFORE
public void removeIndex(ByteBuffer columnName)
{
    indexCfs.invalidate();
}
```

```java
// AFTER
public void removeIndex(ByteBuffer columnName)
{
    // interrupt in-progress compactions
    Collection<ColumnFamilyStore> cfss = Collections.singleton(indexCfs);
    CompactionManager.instance.interruptCompactionForCFs(cfss, true);
    CompactionManager.instance.waitForCessation(cfss);

    indexCfs.keyspace.writeOrder.awaitNewBarrier();
    indexCfs.forceBlockingFlush();

    indexCfs.readOrdering.awaitNewBarrier();
    indexCfs.invalidate();
}
```

---

## How to Spot

Any `invalidate()` or `drop()` call on a storage component that manages files should be audited for whether all concurrent writers, compactors, and asynchronous deletions have been quiesced first. The pattern of "stop, fence, flush, fence, then invalidate" — using memory/write barriers around each boundary — is the required sequence. When a drop is immediately followed by a recreate, the window between async deletion and fresh initialization is particularly dangerous.

---

## Test

A new test `testGetTemporaryFilesSafeAfterObsoletion` in `TransactionLogsTest` races `getTemporaryFiles` against asynchronous log file deletion 1000 times to verify no exception is thrown. `TrackerTest` was also updated to use `waitForDeletions()` instead of the now-removed `pauseDeletions` mechanism.
