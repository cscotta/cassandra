# Bug 0248: SSTable Loader Misidentifies Keyspaces Named "snapshots" or "backups"

**Commit**: 6e6846892a · **Ticket**: CASSANDRA-14013
**Subsystem**: `io/sstable/Descriptor.java`, `io/sstable/SSTableLoader.java`
**Category**: Logic error in condition

---

## Root Cause

`Descriptor.fromFilename()` determined whether an SSTable lived inside a snapshot or backup subdirectory by checking only the directory name, without verifying that the parent directory actually looked like a table directory (i.e., contained a UUID dash segment). This meant that if a legitimate keyspace happened to be named `snapshots` or `backups`, its directory was mistakenly treated as a special subdirectory and the path traversal walked one level too far up, producing a wrong keyspace/table name. `SSTableLoader` had a separate fragile workaround for the `backups` case (CASSANDRA-16235) that was removed once `Descriptor` was fixed.

### Before / After

```java
// BEFORE
if (tableDir.getName().equals(Directories.BACKUPS_SUBDIR))
    tableDir = tableDir.getParentFile();
else if (parentOf(name, tableDir).getName().equals(Directories.SNAPSHOT_SUBDIR))
    tableDir = parentOf(name, parentOf(name, tableDir));
```

```java
// AFTER
if (tableDir.getName().equals(Directories.BACKUPS_SUBDIR) && tableDir.getParentFile().getName().contains("-"))
    tableDir = tableDir.getParentFile();
else {
    File keyspaceOrSnapshotDir = parentOf(name, tableDir);
    if (keyspaceOrSnapshotDir.getName().equals(Directories.SNAPSHOT_SUBDIR)
        && parentOf(name, keyspaceOrSnapshotDir).getName().contains("-"))
        tableDir = parentOf(name, keyspaceOrSnapshotDir);
}
```

---

## How to Spot

Whenever directory-tree path parsing uses only a directory's own name to infer its semantic role, ask whether a user-controlled name (keyspace, table, snapshot tag) could collide with the special directory name. The fix pattern is to add an additional structural check — here, verifying that the sibling directory contains a dash (UUID) to confirm it really is a table directory — before ascending the path.

---

## Test

Tests in `DescriptorTest` and `SSTableLoaderTest` create descriptors and load SSTables from directories whose keyspace is literally named `snapshots` or `backups` and assert that the correct keyspace/table names are resolved.
