# Bug 0315: Snapshot True Size Always Zero Due to File-Object Identity Mismatch

**Commit**: 95a6223057 · **Ticket**: CASSANDRA-17267
**Subsystem**: src/java/org/apache/cassandra/db/Directories.java
**Category**: Incorrect filtering / wrong query result

---

## Root Cause

`SSTableSizeSummer` is supposed to exclude snapshot-linked files from the "true size" calculation so that only on-disk data not already covered by a snapshot is counted.  The exclusion set was a `HashSet<File>`, and the `File` objects in the set came from a different code path than the `File` objects produced during directory traversal.  Because `File.equals()` compares the full path string, two `File` instances representing the same filename in different directories (live data vs. snapshot hardlink) were never equal, so no file was ever excluded.  The fix changes the set to hold only the bare filename (`String`) and compares by name alone, which correctly matches hardlinked snapshot copies back to their live counterparts.

### Before / After

```java
// BEFORE
private final HashSet<File> toSkip;
SSTableSizeSummer(File path, List<File> files)
{
    super(path);
    toSkip = new HashSet<>(files);
}
// ...
&& !toSkip.contains(file);
```

```java
// AFTER
private final Set<String> toSkip;
SSTableSizeSummer(File path, List<File> files)
{
    super(path);
    toSkip = files.stream().map(f -> f.getName()).collect(Collectors.toSet());
}
// ...
&& !toSkip.contains(file.getName());
```

---

## How to Spot

When a `Set` is used to exclude elements during a traversal, verify that the set members and the traversal elements use the same identity concept.  `File` equality is path-based, so any change in directory prefix (e.g., live vs. snapshot directory) makes the lookup always miss.  Prefer storing the key field—name, inode, or canonical path—that is stable across directory context.

---

## Test

Adds `testSnapshotSize()` in `ColumnFamilyStoreTest`: writes a row, flushes, takes a snapshot, and asserts `trueSize == 0`; then compacts, waits for deletions, and asserts `trueSize > 0` while `sizeOnDisk > trueSize`.
