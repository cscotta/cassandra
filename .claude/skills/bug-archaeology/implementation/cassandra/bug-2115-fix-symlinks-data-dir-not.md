# Bug 2115: Symlinks under data directory not recognized due to canonical path resolution

**Commit**: 7752f018b1 · **Ticket**: CASSANDRA-5185
**Subsystem**: src/java/org/apache/cassandra/db/Directories.java
**Category**: Logic error in condition

---

## Root Cause

`Directories.getLocationForDisk()` matched sstable directories to their data directory by comparing canonical paths. When the data directory itself (or an sstable subdirectory) was a symlink, `getCanonicalPath()` resolved the symlink to its real filesystem target, making the prefix check fail even though logically the path belonged to that data directory. Switching to `getAbsolutePath()` preserves the symlink path as configured, so the prefix relationship is evaluated correctly.

### Before / After

```java
// BEFORE
if (FileUtils.getCanonicalPath(dir).startsWith(FileUtils.getCanonicalPath(dataDirectory)))
    return dir;
```

```java
// AFTER
if (dir.getAbsolutePath().startsWith(dataDirectory.getAbsolutePath()))
    return dir;
```

---

## How to Spot

Any path-prefix comparison that uses `getCanonicalPath()` will silently break when symlinks are involved, because the resolved paths may share no common prefix even if the configured paths do. Prefer `getAbsolutePath()` for configuration-level path comparisons; reserve canonical resolution for deduplication or security checks.

---

## Test

No test added.
