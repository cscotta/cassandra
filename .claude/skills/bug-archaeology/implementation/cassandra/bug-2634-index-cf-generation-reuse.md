# Bug 2634: Index CF SSTable generation numbers reused after drop/recreate causing file collisions

**Commit**: 7f9f79622a · **Ticket**: CASSANDRA-2872

## Detection Rule
When computing the next SSTable generation number for a column family, scan all data directories (including backups) for any SSTable file whose `cfname` matches, rather than relying on a method that may omit index column family files.

## Code Smell
`ColumnFamilyStore.fileIndexGenerator()` calls `files(table.name, columnFamily, true)` which filters by the CF name using the directory-scanning path but may not pick up all files on disk (especially after an index CF is dropped and recreated), allowing generation numbers to be reused and SSTable files to collide.

## Tags
- scope: cassandra
- category: state-not-cleaned
- code-pattern: generation-number-reuse
- severity: data-loss
- subsystem: storage-engine

## Root Cause
When computing the maximum SSTable generation number to avoid conflicts, the old `files(table.name, columnFamily, true)` helper used a `FilenameFilter` that only looked in the main data directory and did not properly enumerate all CF variants (particularly index CFs whose names end with a suffix). After a secondary index CF was dropped and recreated, the new scan missed the old files on disk, so the new CF started reusing generation numbers that were still present as files, causing SSTable file overwrites and potential data corruption.

## Example
```java
// BEFORE
for (Descriptor desc : files(table.name, columnFamily, true).keySet()) {
    generations.add(desc.generation);
}

// AFTER – scan all data paths, filter by cfname explicitly
for (String path : DatabaseDescriptor.getAllDataFileLocationsForTable(table.name)) {
    for (Pair<Descriptor, Component> pair : files(new File(path))) {
        Descriptor desc = pair.left;
        if (!desc.cfname.equals(columnFamily)) continue;
        generations.add(desc.generation);
    }
}
```
