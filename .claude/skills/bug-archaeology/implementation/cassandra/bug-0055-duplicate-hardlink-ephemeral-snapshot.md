# Bug 0055: Duplicate Hardlink Error when Forcefully Re-taking Ephemeral Repair Snapshot

**Commit**: 6cfea18dda · **Ticket**: CASSANDRA-20490
**Subsystem**: src/java/org/apache/cassandra/service/snapshot, src/java/org/apache/cassandra/io/sstable/format/SSTableReader.java
**Category**: State not cleaned up

---

## Root Cause

During incremental repair, `snapshot()` can be called multiple times with the same snapshot name (forced re-snapshot). The `SnapshotManager.prePopulateSnapshots` method threw a `RuntimeException` if a snapshot with that tag already existed, even for ephemeral snapshots where overwriting is safe and expected. Additionally, `SSTableReader.createLinks` called `FileUtils.createHardLink`, which also threw unconditionally if the target hard link already existed. There was no path to tolerate duplicate links for the ephemeral case. The fix introduces a `DuplicateHardlinkException` subtype, makes `createLinks` accept a `force` flag (set for ephemeral snapshots) that silently absorbs the duplicate-link error, and makes `prePopulateSnapshots` allow overwriting an existing ephemeral snapshot rather than throwing.

### Before / After

```java
// BEFORE – always throws on duplicate, no ephemeral exception
if (snapshots.contains(toCreateEntry.getValue()))
    throw new RuntimeException("Snapshot ... already exists.");

// and in createLinks:
FileUtils.createHardLink(sourceFile, targetLink);  // throws on existing link
```

```java
// AFTER – ephemeral snapshots silently overwrite; force flag absorbs duplicate hardlink
if (existingSnapshot.equals(snapshotToCreate)) {
    if (!task.options.ephemeral)
        throw new RuntimeException("Snapshot ... already exists.");
    // else: overwrite is fine for ephemeral
}

// and in createLinks:
try {
    FileUtils.createHardLink(sourceFile, targetLink);
} catch (DuplicateHardlinkException ex) {
    if (!force) throw new RuntimeException(ex.getMessage());
}
```

---

## How to Spot

When a "take snapshot" or "create links" operation can be retried or called concurrently (e.g., repair forced re-snapshot), the code must distinguish between operations where idempotency is required (ephemeral, internally managed) and those where a duplicate truly indicates a bug (user-named snapshots). Review callers of `createHardLink` or similar file-creation methods to confirm they handle `EEXIST` gracefully when the operation is expected to be idempotent.

---

## Test

`ActiveRepairServiceTest.testForcedSnapshot` calls `snapshot()` twice with the same repair session ID and asserts that only one snapshot exists and no exception is thrown.
