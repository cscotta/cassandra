# Bug 2263: NPE When Listing Data Directories During Migration

**Commit**: 6e1f3a0195 · **Ticket**: CASSANDRA-4572
**Subsystem**: src/java/org/apache/cassandra/db/Directories.java
**Category**: Missing null / bounds check

---

## Root Cause

`Directories` contains migration helpers (`migrateDataDirs`, `migrateSnapshots`, `migrateBackups`) that enumerate filesystem entries using `File.listFiles()`. Per the Java contract, `File.listFiles()` returns `null` — not an empty array — when the path does not denote a directory or an I/O error occurs. All three methods iterated directly over the return value without a null check, causing a `NullPointerException` whenever a data, snapshot, or backup directory was inaccessible or had already been removed. The fix assigns each `listFiles()` result to a local variable and wraps the iteration in a null guard.

### Before / After

```java
// BEFORE – direct iteration, NPE if listFiles() returns null
for (File ksDir : location.listFiles())
{
    ...
    for (File file : ksDir.listFiles())
        migrateFile(file, ksDir, null);
}
```

```java
// AFTER – null-safe iteration
File[] ksDirs = location.listFiles();
if (ksDirs != null)
{
    for (File ksDir : ksDirs)
    {
        ...
        File[] files = ksDir.listFiles();
        if (files != null)
        {
            for (File file : files)
                migrateFile(file, ksDir, null);
        }
    }
}
```

---

## How to Spot

Any call to `File.listFiles()` whose return value is directly used in a for-each loop without a null check is a latent NPE. This is a well-known Java pitfall — treat `listFiles()` the same as any nullable return value and always guard against null before iteration.

---

## Test

No test added.
