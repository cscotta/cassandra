# Bug 0837: Directory Sync Missing Before SSTable File Deletion Causes Spurious NoSuchFileException

**Commit**: 9ccccf28dc · **Ticket**: CASSANDRA-12261
**Subsystem**: SSTable lifecycle / LogTransaction
**Category**: Non-atomic file write

---

## Root Cause
`LogFile.delete()` called `deleteFilesForRecordsOfType()` without first syncing the parent directory. `SSTableTider` had already hard-deleted certain SSTable components and synced them, but those deletions were not yet visible to `record.getExistingFiles()` inside `deleteRecordFiles()` because the directory entry changes had not been flushed to disk. As a result, `LogTransaction.delete()` would attempt to delete a file it believed still existed but that had already been removed, producing a `NoSuchFileException`. Separately, `SSTableTider.run()` was unconditionally calling `delete(datafile)` without checking whether the data file still existed. Both problems are fixed: a directory sync is inserted before content deletion in `LogFile.delete()`, and `SSTableTider.run()` now guards the `delete(datafile)` call with an `exists()` check.

### Before / After
```java
// BEFORE — LogFile.delete()
deleteFilesForRecordsOfType(committed() ? Type.REMOVE : Type.ADD);

// AFTER — LogFile.delete()
Throwables.maybeFail(syncDirectory(accumulate)); // sync first
deleteFilesForRecordsOfType(committed() ? Type.REMOVE : Type.ADD);

// BEFORE — SSTableTider.run()
delete(datafile);

// AFTER — SSTableTider.run()
if (datafile.exists())
    delete(datafile);
else if (!wasNew)
    logger.error("SSTableTider ran with no existing data file ...");
```

---

## How to Spot
Anytime a multi-step file-lifecycle sequence assumes that a previous deletion is immediately visible to a subsequent directory scan, look for a missing directory sync between those two steps. The invariant "sync the directory before enumerating its contents for deletion" prevents stale cache entries from producing spurious not-found errors.

---

## Test
No test added in this commit.
