# Bug 1919: Disk Space Check Blocks Writes When Space Is Available

**Commit**: 7161aec42c · **Ticket**: CASSANDRA-5605
**Subsystem**: src/java/org/apache/cassandra/db/Directories.java
**Category**: Logic error in condition

---

## Root Cause

The old `getLocationWithMaximumAvailableSpace(estimatedSize)` method compared an estimated future file size against 90% of the largest available disk partition. If the estimate exceeded that threshold the method returned null and the operation (compaction, flush, streaming) was refused even when significant real space remained. This "second-guessing" produced false positives: legitimate writes were rejected because the pre-flight size estimate was wrong or because the 0.9 load-factor was too conservative, while the actual disk write would have succeeded. The blacklisting mechanism already handles genuinely full disks, so the upfront size check was redundant and harmful.

The fix removes the size-based gating entirely. `getWriteableLocation()` now simply picks the non-blacklisted directory with the most free space and least pending work, throwing `IOError` only when every directory is blacklisted. Callers no longer pass an estimated size.

### Before / After

```java
// BEFORE
public File getDirectoryForNewSSTables(long estimatedSize) {
    File path = getLocationWithMaximumAvailableSpace(estimatedSize);
    ...
}

// getLocationWithMaximumAvailableSpace returned null if estimatedSize > 0.9 * maxFree
```

```java
// AFTER
public File getDirectoryForNewSSTables() {
    File path = getWriteableLocationAsFile();
    ...
}

// getWriteableLocation throws IOError only when all directories are blacklisted
```

---

## How to Spot

Pre-flight capacity checks based on estimated file sizes are fragile: estimates are often wrong, and the check duplicates logic already handled by the error/blacklist path. Prefer letting the OS report actual failures and handle them in the error path rather than pre-rejecting operations based on speculative size comparisons.

---

## Test

Existing unit tests in `DirectoriesTest`, `ScrubTest`, `SSTableReaderTest`, `SSTableSimpleWriterTest`, and `ColumnFamilyStoreTest` were updated to call the new no-argument `getDirectoryForNewSSTables()` signature.
