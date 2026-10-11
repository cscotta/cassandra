# Bug 0595: Scan response builder accepted delete files unrelated to file scan tasks

**Commit**: ba28a3365a1a0e3cd64875c5d76191080799e8bf · **Ticket**: #14838

## Detection Rule

When a builder API accepts both a collection of parent objects (e.g. FileScanTasks) and a separately-settable collection of their child/associated objects (e.g. DeleteFiles), verify that the child collection is always derived exclusively from the parent objects and that setting both independently is either impossible or validated at build time.

## Code Smell

A builder with both `withFileScanTasks(List<FileScanTask>)` and `withDeleteFiles(List<DeleteFile>)` setters where each mutates a separate field, allowing callers to supply delete files that have no relationship to the supplied scan tasks, and where the server-side code explicitly assembled the delete list from scan tasks yet still called `withDeleteFiles()` with a redundant (or wrong) list.

## Tags

- scope: universal
- category: wrong-filter-result
- code-pattern: independent-setter-bypasses-invariant
- severity: silent-wrong-result
- subsystem: rest-catalog / scan-planning

## Root Cause

`BaseScanTaskResponse.Builder` exposed independent `withFileScanTasks` and `withDeleteFiles` setters. Because both were independent, a caller could pass file scan tasks referencing delete file A while `withDeleteFiles` supplied delete file B, producing a response whose `deleteFiles` list was inconsistent with the embedded task references. The server did compute delete files from tasks in `CatalogHandlers`, but the duplicated call to `withDeleteFiles` was redundant and fragile — any divergence between the two sources would silently produce incorrect results. The fix derives `deleteFiles` automatically from `fileScanTasks` inside `withFileScanTasks`, deprecates the independent setter, and adds a parser-level guard that rejects responses carrying delete files without accompanying file scan tasks.

## Example

```java
// BEFORE — caller can supply unrelated delete files; no invariant enforced
public B withFileScanTasks(List<FileScanTask> tasks) {
    this.fileScanTasks = tasks;
    return self();
}

public B withDeleteFiles(List<DeleteFile> deleteFilesList) {
    this.deleteFiles = deleteFilesList;   // fully independent
    return self();
}

// Server-side usage: redundant, and any mismatch goes unnoticed
.withFileScanTasks(tasks)
.withDeleteFiles(tasks.stream().flatMap(t -> t.deletes().stream())
    .distinct().collect(Collectors.toList()))
```

```java
// AFTER — delete files derived automatically; standalone deleteFiles rejected
public B withFileScanTasks(List<FileScanTask> tasks) {
    this.fileScanTasks = tasks;
    if (tasks != null) {
        this.deleteFiles = DeleteFileSet.of(
            () -> tasks.stream().flatMap(t -> t.deletes().stream()).iterator());
    }
    return self();
}

@Deprecated
public B withDeleteFiles(List<DeleteFile> deleteFilesList) {
    this.deleteFiles = DeleteFileSet.of(deleteFilesList);
    return self();
}
```
