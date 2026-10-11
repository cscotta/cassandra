# Bug 0723: NPE in legacy-file cleanup when directory listing fails on upgrade

**Commit**: 849f8cd616 · **Ticket**: CASSANDRA-13389
**Subsystem**: SystemKeyspace / startup / upgrade
**Category**: Missing null / bounds check

---

## Root Cause
During the 3.0/3.x upgrade path, `SystemKeyspace.migrateDataDirs()` scanned data directories for legacy-format files by calling `File.listFiles()` on each level of the directory tree. `File.listFiles()` returns `null` (rather than throwing) when an I/O error occurs or when the path is not a directory. The code passed the null array directly to a for-each loop and to `FileUtils.delete(File...)`, both of which then threw a `NullPointerException`. The fix introduces `FileUtils.visitDirectory()`, which uses `Files.newDirectoryStream()` and catches `IOException`/`DirectoryIteratorException` instead of silently returning null, and adds an explicit null-guard in `FileUtils.delete()` for callers that still use the old API.

### Before / After
```java
// BEFORE — File.listFiles() can return null on I/O error
for (File ksdir : dir.listFiles((d, n) -> new File(d, n).isDirectory()))
{
    for (File cfdir : ksdir.listFiles(...))
    {
        FileUtils.delete(cfdir.listFiles(...));  // NPE if listFiles returns null
    }
}

// AFTER — visitDirectory() uses DirectoryStream; handles IOException explicitly
visitDirectory(dir.toPath(), File::isDirectory, ksdir ->
    visitDirectory(ksdir.toPath(), File::isDirectory, cfdir ->
        visitDirectory(cfdir.toPath(), Descriptor::isLegacyFile, FileUtils::delete)));

// Also added in FileUtils.delete():
if (files == null) { logger.debug("Received null list of files to delete"); return; }
```

---

## How to Spot
Any call to `File.listFiles()` without a null check is a latent NPE waiting for an I/O error. The Java documentation explicitly states that `listFiles()` returns null when an I/O error occurs or when the abstract pathname is not a directory. Prefer `Files.newDirectoryStream()` with an explicit exception handler, or at minimum check the return value of `listFiles()` before iterating.

---

## Test
`SystemKeyspaceTest` gains a test that simulates an inaccessible directory during `migrateDataDirs()` and verifies that the method completes without throwing instead of producing an NPE.
