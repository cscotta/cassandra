# Bug 2762: Cache Save Fails on Windows Because Old File Is Not Deleted Before Rename

**Commit**: f4a68eb778 · **Ticket**: CASSANDRA-2207

## Detection Rule
When atomically replacing a file via `tmpFile.renameTo(path)`, explicitly delete the destination file first on Windows, which does not allow renaming onto an existing file.

## Code Smell
`tmpFile.renameTo(path)` is called without first calling `path.delete()`; on Windows this returns `false` when the destination already exists, causing the cache to never be persisted.

## Tags
- scope: cassandra
- category: logic-error
- code-pattern: rename-without-delete-on-windows
- severity: silent-wrong-result
- subsystem: storage-engine

## Root Cause
POSIX `rename()` atomically replaces the destination, but Windows does not. `CacheWriter.saveCache` called `tmpFile.renameTo(path)` directly; on Windows, if a previous cache file existed, this always returned `false` and threw an `IOException`. The fix adds `path.delete()` (ignoring the result if the file doesn't exist) before renaming.

## Example
```java
// BEFORE
if (!tmpFile.renameTo(path))
    throw new IOException("Unable to rename cache to " + path);
```
```java
// AFTER
path.delete(); // ignore error if it didn't exist
if (!tmpFile.renameTo(path))
    throw new IOException("Unable to rename " + tmpFile + " to " + path);
```
