# Bug 0244: NoSuchFileException When Concurrently Removing a Snapshot

**Commit**: cfe9641fbe · **Ticket**: CASSANDRA-18211
**Subsystem**: db/Directories, service/snapshot/SnapshotManager
**Category**: Missing null / bounds check

---

## Root Cause

`Directories.snapshotDetails()` and `SnapshotManager` could trigger a `NoSuchFileException` (wrapped in a `RuntimeException`) when deleting a snapshot directory that had already been removed concurrently (e.g., by another thread, an `auto_snapshot` cleanup, or a parallel `nodetool clearsnapshot`). The original catch block only caught `FSWriteError` and re-threw it; a `NoSuchFileException` wrapped as a generic `RuntimeException` would propagate uncaught. The fix broadens the catch to `RuntimeException`, then checks whether the snapshot directory still exists: if it does not, the deletion succeeded from another path so the exception is silently swallowed; otherwise it is re-thrown. Additionally, `FileUtils.folderSize()` was hardened to return 0 when the folder does not exist and to skip `NoSuchFileException` during directory traversal.

### Before / After

```java
// BEFORE
catch (FSWriteError e)
{
    throw e;
}
```

```java
// AFTER
catch (RuntimeException ex)
{
    if (!snapshotDir.exists())
        return; // ignore
    throw ex;
}
```

---

## How to Spot

Any deletion code that does not handle the TOCTOU window between "check existence" and "delete" is susceptible to this class of error. When deleting a resource that may be concurrently removed, the correct idiom is to catch the "not found" exception and treat it as a successful deletion. Review every `deleteRecursive` or `Files.delete` call site and ensure the surrounding catch also handles `NoSuchFileException` or checks post-deletion existence.

---

## Test

The existing `SnapshotsTest` distributed test and `SnapshotManagerTest` unit test were updated to cover concurrent snapshot removal scenarios, asserting that no exception propagates when the directory disappears during deletion.
