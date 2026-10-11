# Bug 0461: Snapshot Manifest Overwritten for Each Index CFS

**Commit**: 976096abd2 · **Ticket**: CASSANDRA-10968
**Subsystem**: `src/java/org/apache/cassandra/db/ColumnFamilyStore.java`
**Category**: State not cleaned up

---

## Root Cause

In `snapshotWithoutFlush`, the `JSONArray` that accumulates the list of SSTable files for the manifest was declared inside the `for (ColumnFamilyStore cfs : concatWithIndexes())` loop, and `writeSnapshotManifest` was called at the end of each loop iteration. This meant that when a table had table-backed secondary indexes, the manifest was written once per CFS (the base table and each index), each time with only the files from that single CFS. The final manifest file on disk contained only the index CFS files from the last iteration, silently discarding the base-table SSTable entries that had been written to earlier manifest versions.

### Before / After

```java
// BEFORE — JSONArray and manifest write inside the loop; manifest overwritten each iteration
for (ColumnFamilyStore cfs : concatWithIndexes()) {
    final JSONArray filesJSONArr = new JSONArray();
    try (RefViewFragment currentView = ...) {
        // ... add ssTable files to filesJSONArr ...
        writeSnapshotManifest(filesJSONArr, snapshotName); // overwrites previous
    }
}
```

```java
// AFTER — accumulate all files first, write manifest once after the loop
final JSONArray filesJSONArr = new JSONArray();
for (ColumnFamilyStore cfs : concatWithIndexes()) {
    try (RefViewFragment currentView = ...) {
        // ... add ssTable files to filesJSONArr ...
    }
}
writeSnapshotManifest(filesJSONArr, snapshotName);
```

---

## How to Spot

When a write-to-file call (such as manifest or metadata flush) appears inside a loop that iterates over both a primary store and its indexes, the final file will contain only the data from the last iteration. Reviewers should check that any operation producing a single aggregated output file (manifest, summary, etc.) accumulates state across all loop iterations before writing, rather than writing incrementally inside the loop.

---

## Test

`testSnapshotWithoutFlushWithSecondaryIndexes` inserts data into `Keyspace1.Indexed1` (a table with a secondary index), calls `snapshotWithoutFlush`, parses the written manifest JSON, and asserts that the `files` array contains exactly two entries — one for the base table SSTable and one for the index CFS SSTable.
