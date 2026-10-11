# Bug 1011: Data-dir migration FileFilter uses wrong isDirectory check

**Commit**: d0da9e7612 · **Ticket**: CASSANDRA-10788
**Subsystem**: SystemKeyspace / upgrade
**Category**: Logic error in condition

---

## Root Cause
`SystemKeyspace.migrateDataDirs()` iterates data directories looking for legacy SSTable files to delete during a 2.x-to-3.0 upgrade.  The `FilenameFilter` lambda used to select subdirectories was `(d, n) -> d.isDirectory()` — this tests whether the *parent* directory `d` is a directory (which is always true inside `File.listFiles`), not whether the child entry named `n` is a directory.  As a result the filter accepted every entry including plain files, causing `NullPointerException` or incorrect traversal when a non-directory entry was passed to a subsequent `listFiles` call.  The fix replaces the predicate with `(d, n) -> new File(d, n).isDirectory()`, constructing the full child `File` before calling `isDirectory()`.

### Before / After
```java
// BEFORE
for (File ksdir : dir.listFiles((d, n) -> d.isDirectory()))
{
    for (File cfdir : ksdir.listFiles((d, n) -> d.isDirectory()))
    { ... }
}

// AFTER
for (File ksdir : dir.listFiles((d, n) -> new File(d, n).isDirectory()))
{
    for (File cfdir : ksdir.listFiles((d, n) -> new File(d, n).isDirectory()))
    { ... }
}
```

---

## How to Spot
`FilenameFilter` lambdas receive `(File dir, String name)` — the first parameter is the *parent* directory, not the child being tested.  Any lambda of the form `(d, n) -> d.isXxx()` or `(d, n) -> d.something` that ignores `n` is almost certainly wrong.  The correct pattern is `(d, n) -> new File(d, n).isXxx()`.

---

## Test
`SystemKeyspaceTest.testMigrateEmptyDataDirs()` added — creates an empty keyspace directory, a keyspace with an empty table subdirectory, and a keyspace directory containing a plain file, and asserts that `migrateDataDirs()` completes without error and reports zero legacy files in all cases.
