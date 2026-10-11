# Bug 0310: Hadoop trash moves silently suppress actual file deletion

**Commit**: a97b4ecc4db52ee2b0ddcecd34219275df002876 · **Ticket**: #15386

## Detection Rule

When a deletion API is replaced with a "soft delete" (trash, recycle bin, versioned delete marker), verify that all callers that depend on the file being gone after the call — including existence checks, bulk-deletion error accounting, and prefix deletion — are updated accordingly.

## Code Smell

A `deletePath()` helper that calls `trash.moveToTrash(path)` for some `FileSystem` subtypes but `fs.delete(path, recursive)` for others, where the recursive flag is always passed to `fs.delete()` but `moveToTrash()` has no recursive parameter — meaning directory trees are handled differently across code paths.

## Tags

- scope: project-specific
- category: wrong-filter-result
- code-pattern: soft-delete-not-delete
- severity: silent-wrong-result
- subsystem: hadoop / file-io / deletion

## Root Cause

The `deletePath()` helper used `trash.moveToTrash(path)` for `LocalFileSystem` and `DistributedFileSystem` when trash was configured, but `moveToTrash()` moves files to a `.Trash` directory rather than deleting them. Iceberg's `FileIO.deleteFile()` and `deletePrefix()` contracts guarantee the path is removed from its original location. Code that tested for file existence after deletion, or that expected `BulkDeletionFailureException` on failures, would silently observe wrong state because the file still existed at its original path during any concurrent read window. The `recursive` flag was also lost when taking the trash path, so directory deletes behaved differently depending on filesystem type.

## Example

```java
// BEFORE
private void deletePath(FileSystem fs, Path toDelete, boolean recursive) throws IOException {
  Trash trash = new Trash(fs, getConf());
  if ((fs instanceof LocalFileSystem || fs instanceof DistributedFileSystem)
      && trash.isEnabled()) {
    trash.moveToTrash(toDelete);  // file still visible at original path; recursive ignored
  } else {
    fs.delete(toDelete, recursive);
  }
}

// callers
deletePath(fs, toDelete, false);      // deleteFile
deletePath(fs, prefixToDelete, true); // deletePrefix
```

```java
// AFTER
// callers directly use fs.delete() — consistent, recursive flag preserved
fs.delete(toDelete, false /* not recursive */);
fs.delete(prefixToDelete, true /* recursive */);
```
