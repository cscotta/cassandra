# Bug 1918: SSTableReader Instances From Snapshots Are Never Closed During Repair

**Commit**: 9d7bb1e0c2 · **Ticket**: CASSANDRA-6047
**Subsystem**: src/java/org/apache/cassandra/db/compaction/CompactionManager.java, src/java/org/apache/cassandra/io/sstable/SSTableReader.java
**Category**: Resource leak

---

## Root Cause

During snapshot-based repair validation, `CompactionManager.doValidationCompaction` opened SSTableReaders from a snapshot via `cfs.getSnapshotSSTableReader`. These readers were not tracked by the `DataTracker` reference-counting mechanism, so calling `SSTableReader.releaseReferences()` on them did nothing useful—the reference count was never incremented in the first place, so file handles (mmapped segments, bloom filter files) were never released. The snapshot was also not cleared after validation completed when the cleanup path hit an exception.

The fix distinguishes snapshot validation from live validation, directly calls `close()` on each snapshot SSTableReader (a new `Closeable` interface added to `SSTableReader`), and then clears the snapshot. Non-snapshot readers continue to use `releaseReferences()` as before.

### Before / After

```java
// BEFORE (finally block)
SSTableReader.releaseReferences(sstables);
iter.close();
if (cfs.snapshotExists(validator.request.sessionid))
    cfs.clearSnapshot(validator.request.sessionid);
```

```java
// AFTER (finally block)
iter.close();
if (isSnapshotValidation) {
    for (SSTableReader sstable : sstables)
        FileUtils.closeQuietly(sstable);
    cfs.clearSnapshot(validator.request.sessionid);
} else {
    SSTableReader.releaseReferences(sstables);
}
```

---

## How to Spot

Whenever an SSTableReader is opened outside the normal `DataTracker` lifecycle (e.g. from a snapshot directory), it bypasses the reference-counting system and must be explicitly closed. Look for code that calls `releaseReferences` on readers that were never registered with a tracker—those calls are silently ineffective and mask a resource leak.

---

## Test

No test added.
