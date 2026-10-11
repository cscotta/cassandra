# Bug 0236: NPE in SnapshotLoader When Data Directory Is One Level From Filesystem Root

**Commit**: 4ac89e6451 · **Ticket**: CASSANDRA-18359
**Subsystem**: src/java/org/apache/cassandra/service/snapshot/SnapshotLoader.java
**Category**: Missing null / bounds check

---

## Root Cause

`SnapshotLoader`'s `FileVisitor.preVisitDirectory` called `subdir.getParent().getFileName()` unconditionally. When the data directory is exactly one level below the filesystem root (e.g. `/data`), `getParent()` returns the root path `/`, and `getFileName()` on a root path returns `null`. The subsequent `.toString()` call on that `null` throws a `NullPointerException` before the snapshot directory can be identified or skipped.

### Before / After

```java
// BEFORE — no guard against root path
if (subdir.getParent().getFileName().toString().equals(SNAPSHOT_SUBDIR))
{
    ...
}
```

```java
// AFTER — skip iteration when parent or its filename is null (root path case)
if (subdir.getParent() == null || subdir.getParent().getFileName() == null)
    return FileVisitResult.CONTINUE;

if (subdir.getParent().getFileName().toString().equals(SNAPSHOT_SUBDIR))
{
    ...
}
```

---

## How to Spot

Any `Path.getFileName()` call must be guarded with a null check because `getFileName()` returns `null` for root paths. When walking a directory tree rooted at a user-configurable path, the visitor will encounter the root and its immediate children as `subdir` values, so both `getParent()` and `getFileName()` can be `null` at the top of the traversal.

---

## Test

No test added; the fix was verified by configuring a data directory one level from root and confirming the snapshot-loading walk completes without NPE.
