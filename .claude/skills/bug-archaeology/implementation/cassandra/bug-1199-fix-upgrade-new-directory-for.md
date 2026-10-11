# Bug 1199: Secondary Index SSTables Not Migrated to Own Subdirectory on Upgrade

**Commit**: 1dbbf6040b · **Ticket**: CASSANDRA-9687
**Subsystem**: src/java/org/apache/cassandra/db/Directories.java
**Category**: State not cleaned up

---

## Root Cause

Starting in Cassandra 2.2, secondary index SSTables were given their own subdirectory (`<cf dir>/.<index name>/`) instead of living alongside the parent CFS files. The `Directories` constructor computed the correct new path for the index, but never actually moved existing pre-2.2 files from the parent directory into the new subdirectory on first startup. As a result, existing index SSTables were invisible to any component that used `Directories` to enumerate index files, effectively losing access to the index data after an upgrade. The fix adds a migration block in the constructor that scans the parent directory for files matching the index's descriptor and renames them into the newly created index subdirectory.

### Before / After

```java
// BEFORE — new path computed but old files never moved
if (indexNameWithDot != null)
{
    for (int i = 0; i < dataDirectories.length; ++i)
        dataPaths[i] = new File(dataPaths[i], indexNameWithDot);
}
// (no migration)
```

```java
// AFTER — old files found in parent dir and renamed into new index dir
if (indexNameWithDot != null)
{
    for (File dataPath : dataPaths)
    {
        File[] indexFiles = dataPath.getParentFile().listFiles(...);
        for (File indexFile : indexFiles)
        {
            File destFile = new File(dataPath, indexFile.getName());
            FileUtils.renameWithConfirm(indexFile, destFile);
        }
    }
}
```

---

## How to Spot

Whenever a directory layout changes between versions, check whether the code that establishes the new path also performs a one-time migration of data from the old location. Computing a new path without moving existing files is a silent data-visibility bug: the files exist on disk but are unreachable through the new path abstraction. Constructor-time migration is the standard Cassandra pattern; look for new path construction that is not paired with a corresponding rename/move block.

---

## Test

`DirectoriesTest.testSecondaryIndexDirectories()` was added to verify that index directories are created under the parent CF directory, that snapshot and backup paths are correctly rooted in the parent, and that snapshot size accounting is accurate for index files.
