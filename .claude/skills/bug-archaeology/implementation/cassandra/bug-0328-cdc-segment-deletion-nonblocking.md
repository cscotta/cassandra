# Bug 0328: Improper CDC Commit Log Segment Deletion in Non-Blocking Mode

**Commit**: 8327744c1a · **Ticket**: CASSANDRA-17233
**Subsystem**: src/java/org/apache/cassandra/db/commitlog
**Category**: State not cleaned up

---

## Root Cause

In CDC non-blocking mode the `CommitLogSegmentManagerCDC` failed in two related ways. First, when a new segment was allocated and the CDC storage limit was exceeded, `processNewSegment` called `deleteOldestLinkedCDCCommitLogSegment` (singular) inside the `cdcStateLock` synchronized block, releasing exactly one file even when multiple files needed to be removed to get under the limit; the loop that should have kept deleting was also inside the lock, creating an incorrect structure. Second, newly allocated segments that were still marked `FORBIDDEN` (from a previous blocking-mode period) were never transitioned to `PERMITTED` in non-blocking mode, so writes to CDC-tracked tables would always throw `CDCWriteException` even though CDC had sufficient space. A third latent defect was that `deleteOldLinkedCDCCommitLogSegment` sorted files by `lastModified` timestamp rather than by commit-log segment ID, meaning it could delete a newer segment while keeping an older one.

### Before / After

```java
// BEFORE – deletion ran once inside the lock; sorted by mtime
List<File> sorted = Arrays.stream(files)
                          .sorted(Comparator.comparingLong(File::lastModified))
                          .collect(Collectors.toList());
File oldestCdcFile = sorted.get(0);
return deleteCDCFiles(oldestCdcFile, cdcIndexFile);
```

```java
// AFTER – sorts by segment ID, deletes until bytesToFree satisfied, runs outside lock
List<File> sorted = Arrays.stream(files)
                          .sorted(new CommitLogSegment.CommitLogSegmentFileComparator())
                          .collect(Collectors.toList());
// loop until bytesDeleted >= bytesToFree or reaches current writing segment
```

```java
// AFTER – segment stuck in FORBIDDEN gets cleared before each allocation attempt
private void ensureSegmentPermittedIfNotBlockWrites(CommitLogSegment segment) {
    if (!DatabaseDescriptor.getCDCBlockWrites() && segment.getCDCState() == CDCState.FORBIDDEN)
        segment.setCDCState(CDCState.PERMITTED);
}
```

---

## How to Spot

When a subsystem has both a "blocking" and a "non-blocking" mode sharing the same state machine, look carefully at transitions that were only designed with the blocking mode in mind: flags set (or left set) under one mode may never be cleared when the other mode is active. Additionally, any file-management loop whose deletion criterion relies on file-system metadata (mtime) rather than a monotonically increasing application-level identifier (segment ID) is fragile; always prefer the application's own ordering.

---

## Test

`CommitLogSegmentManagerCDCTest.testNonblockingShouldMaintainSteadyDiskUsage` writes a large number of CDC-tracked mutations and asserts that total on-disk CDC size does not exceed the configured limit after cleanup.
