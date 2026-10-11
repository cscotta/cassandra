# Bug 0005: Buffered v2 delete files accepted after concurrent format upgrade to v3

**Commit**: 099ef477c8024b3bcb4d12abca18944c25b0eb45 · **Ticket**: #16146

## Detection Rule

When a validation method reads a mutable field (e.g. table format version) at call time rather than at apply/commit time, verify that the same validation is re-run inside `apply()` using the version from the base snapshot being committed against.

## Code Smell

`validateNewDeleteFile()` calls `formatVersion()` (the live table version) when the delete file is buffered, but the buffered list is never re-validated inside `apply()` where `base.formatVersion()` reflects the actual target version at commit time.

## Tags

- scope: distributed-systems
- category: state-not-cleaned
- code-pattern: stale-state-after-lifecycle
- severity: silent-wrong-result
- subsystem: core/snapshot-producer

## Root Cause

`MergingSnapshotProducer.validateNewDeleteFile()` validated each delete file against the table's format version at the moment the file was added to the operation. If another transaction concurrently upgraded the table from v2 to v3 before `apply()` ran, the buffered v2 position-delete files (which are illegal in v3) passed validation and could be committed. The fix extracts a static helper and calls it again inside `apply()` using the base metadata's `formatVersion()`.

## Example

```java
// BEFORE: validation uses live formatVersion(), called only when file is buffered
protected void validateNewDeleteFile(DeleteFile file) {
    Preconditions.checkNotNull(file, "Invalid delete file: null");
    switch (formatVersion()) {   // snapshot of version at buffer time, not commit time
        case 2:
            // allows position delete files
        case 3:
            // requires DVs — but this check was already done at buffer time
    }
}

@Override
public List<ManifestFile> apply(TableMetadata base, Snapshot snapshot) {
    // no re-validation of v2Deletes against base.formatVersion()
    List<ManifestFile> filtered = filterManager.filterManifests(...);
```

```java
// AFTER: re-validate buffered deletes at apply() using base metadata version
private static void validateDeleteFileForVersion(DeleteFile file, int formatVersion) {
    switch (formatVersion) { ... }
}

private void validateDeleteFilesForVersion(int currentFormatVersion) {
    for (DeleteFile file : v2Deletes) {
        validateDeleteFileForVersion(file, currentFormatVersion);
    }
}

@Override
public List<ManifestFile> apply(TableMetadata base, Snapshot snapshot) {
    validateDeleteFilesForVersion(base.formatVersion());
    List<ManifestFile> filtered = filterManager.filterManifests(...);
```
