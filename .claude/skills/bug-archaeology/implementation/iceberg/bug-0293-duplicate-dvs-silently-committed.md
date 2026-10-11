# Bug 0293: Multiple deletion vectors for the same data file committed without merging

**Commit**: de41011180b1e5bd87a12a5177f840c8dface38e · **Ticket**: #15006

## Detection Rule

When a snapshot operation accumulates multiple delete files referencing the same data file (e.g., deletion vectors keyed by `referencedDataFile`), verify that duplicates are merged before being written to the manifest rather than stored as independent entries.

## Code Smell

A `Map` or `Set` keyed by data-file path accumulates incoming delete files without a deduplication/merge step before the manifest is written; multiple `DeleteFile` entries with the same `referencedDataFile` appear in `newDeleteFilesBySpec` or equivalent staging structures.

## Tags

- scope: project-specific
- category: logic-error
- code-pattern: missing-merge-before-commit
- severity: silent-wrong-result
- subsystem: core/snapshot-producer

## Root Cause

`MergingSnapshotProducer` stored incoming deletion vectors (DVs) in a `DeleteFileSet` that was partitioned by spec ID, with no awareness of duplicate DVs targeting the same data file. When multiple DVs for one data file were added in a single operation, all were passed individually to the manifest writer; readers seeing the snapshot might apply only one or interpret them inconsistently, silently missing deletes. The fix groups DVs by `referencedDataFile`, detects duplicates, merges their `PositionDeleteIndex` bitmaps into a single Puffin file, and then writes the merged DV into the manifest.

## Example

```java
// BEFORE: DVs are stored per spec, no per-data-file dedup
private final Map<Integer, DeleteFileSet> newDeleteFilesBySpec = Maps.newHashMap();
private final Set<String> newDVRefs = Sets.newHashSet();

// addDeleteFile just inserts into the set
DeleteFileSet deleteFiles =
    newDeleteFilesBySpec.computeIfAbsent(spec.specId(), ignored -> DeleteFileSet.create());
if (deleteFiles.add(file)) {
    addedFilesSummary.addedFile(spec, file);
    if (ContentFileUtil.isDV(file)) {
        newDVRefs.add(file.referencedDataFile());
    }
}
```

```java
// AFTER: DVs are grouped by referenced data file; duplicates are merged before commit
private final Map<String, List<DeleteFile>> dvsByReferencedFile = Maps.newLinkedHashMap();

if (ContentFileUtil.isDV(file)) {
    dvsByReferencedFile
        .computeIfAbsent(file.referencedDataFile(), k -> Lists.newArrayList())
        .add(file);
} else {
    v2Deletes.add(file);
}
// prepareDeleteManifests() calls mergeDVs() which reads, unions, and rewrites DVs
```
