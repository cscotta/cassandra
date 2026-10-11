# Bug 0145: DV conflict validation scans all delete manifests without partition pruning

**Commit**: 79d4fc82cf9ef2266fd53beabd8a744789744bed · **Ticket**: #15653

## Detection Rule

When a conflict-detection filter is available during snapshot validation, verify that manifest-level partition pruning is applied before iterating manifests, not just row-level filtering inside the manifest entries.

## Code Smell

A `Tasks.foreach(newDeleteManifests)` loop that iterates over the full manifest list before applying a `conflictDetectionFilter`, where the filter could have been used to skip entire manifests via `ManifestEvaluator`.

## Tags

- scope: project-specific
- category: wrong-filter-result
- code-pattern: missing-manifest-partition-prune
- severity: performance
- subsystem: core/snapshot-producer

## Root Cause

`MergingSnapshotProducer.validateDVs()` iterated all newly added delete manifests when checking for concurrent DV conflicts, even when a `conflictDetectionFilter` narrowed the affected partitions. The conflict detection filter was only used at the row level inside each manifest; no manifest-level `ManifestEvaluator` was applied first. In tables with many partitions, this caused every delete manifest to be opened and scanned regardless of whether its partition overlapped the write's filter.

## Example

```java
// BEFORE
Tasks.foreach(newDeleteManifests)
    .stopOnFailure()
    .throwFailureWhenFinished()
    .executeWith(workerPool())
    ...
```

```java
// AFTER
Iterable<ManifestFile> matchingManifests =
    Iterables.filter(
        filterManifestsByPartition(base, conflictDetectionFilter, newDeleteManifests),
        ManifestFile::hasAddedFiles);

Tasks.foreach(matchingManifests)
    .stopOnFailure()
    .throwFailureWhenFinished()
    .executeWith(workerPool())
    ...
```
