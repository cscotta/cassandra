# Bug 0195: AlreadyExistsException when rewrite_table_path encounters duplicate delete files

**Commit**: 9057bf32e94278262cc0084b0d043e757c007f12 · **Ticket**: #15616

## Detection Rule

When collecting items from multiple manifests or snapshots into a `Set` where the items are domain objects (e.g., `DeleteFile`, `DataFile`) that do not override `equals()`/`hashCode()`, verify that a path-identity-aware collection type is used instead of `HashSet` to guarantee correct deduplication.

## Code Smell

A `.collect(Collectors.toSet())` applied to a stream of `DeleteFile` (or other `ContentFile`) objects that may be different Java instances representing the same underlying file path; `HashSet` uses object identity so duplicates are not eliminated.

## Tags

- scope: java
- category: wrong-filter-result
- code-pattern: identity-hash-dedup-failure
- severity: crash
- subsystem: spark/actions/RewriteTablePathSparkAction

## Root Cause

`RewriteTablePathSparkAction` collects position delete files from manifest entries into a `HashSet<DeleteFile>`. The same physical delete file can appear in multiple manifest entries (e.g., when it was added to more than one snapshot), yielding multiple distinct `DeleteFile` object instances for the same path. Because `DeleteFile` does not override `equals()`/`hashCode()`, `HashSet` treats them as different objects; `rewritePositionDeletes` then tries to copy the same file twice, and the second attempt throws `AlreadyExistsException`. The fix replaces `Collectors.toSet()` with `Collectors.toCollection(DeleteFileSet::create)`, which deduplicates by file path.

## Example

```java
// BEFORE — HashSet fails to deduplicate: different object instances, same file path
Set<DeleteFile> deleteFiles =
    rewriteManifestResult.toRewrite().stream()
        .filter(e -> e instanceof DeleteFile)
        .map(e -> (DeleteFile) e)
        .collect(Collectors.toSet());  // identity equality — duplicates survive
```

```java
// AFTER — DeleteFileSet deduplicates by file path
Set<DeleteFile> deleteFiles =
    rewriteManifestResult.toRewrite().stream()
        .filter(e -> e instanceof DeleteFile)
        .map(e -> (DeleteFile) e)
        .collect(Collectors.toCollection(DeleteFileSet::create));
```
