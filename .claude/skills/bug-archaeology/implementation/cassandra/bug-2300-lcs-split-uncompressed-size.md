# Bug 2300: LCS Splits SSTables Based on Uncompressed Size Instead of On-Disk Size

**Commit**: 787e0e6488 · **Ticket**: CASSANDRA-4419

## Detection Rule

When enforcing a file-size limit for compressed files, verify that size comparisons use the compressed on-disk byte count rather than the logical (uncompressed) write pointer, which can be arbitrarily larger.

## Code Smell

`newSSTableSegmentThresholdReached(writer, position)` where `position` is the uncompressed logical file pointer passed from the compaction loop — for compressed SSTables this value far exceeds the actual on-disk size, causing premature or incorrect segment splits.

## Tags

- scope: cassandra
- category: logic-error
- code-pattern: uncompressed-size-used-for-disk-limit
- severity: performance
- subsystem: compaction

## Root Cause

`LeveledCompactionTask` overrode `newSSTableSegmentThresholdReached` to split output SSTables when the write position exceeded the configured size limit. The caller passed the logical (uncompressed) file pointer as `position`. For compressed SSTables the uncompressed position grows much faster than on-disk size, so the threshold was hit far too early, creating many small SSTables. The fix adds `getOnDiskFilePointer()` to `SequentialWriter` (falling through to logical size for uncompressed) and overrides it in `CompressedSequentialWriter` to return the actual compressed file position.

## Example

```java
// BEFORE — uses logical/uncompressed position
protected boolean newSSTableSegmentThresholdReached(SSTableWriter writer, long position) {
    return position > sstableSizeInMB * 1024L * 1024L;  // position = uncompressed offset
}
```

```java
// AFTER — uses actual on-disk compressed position
protected boolean newSSTableSegmentThresholdReached(SSTableWriter writer) throws IOException {
    return writer.getOnDiskFilePointer() > sstableSizeInMB * 1024L * 1024L;
}
// CompressedSequentialWriter.getOnDiskFilePointer() returns out.getFilePointer() (compressed)
```
