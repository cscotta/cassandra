# Bug 1339: SSTableReader.last Does Not Correspond Exactly with File End, Causing Range Scan Overlap

**Commit**: 0ced7a345c · **Ticket**: CASSANDRA-8750
**Subsystem**: src/java/org/apache/cassandra/io/sstable/SSTableWriter.java, io/util/RandomAccessReader.java, io/compress/CompressionMetadata.java
**Category**: Off-by-one

---

## Root Cause

When an SSTable was opened early (during compaction), the `last` key stored in `SSTableReader` did not correspond precisely to the physical end of the written data. `RandomAccessReader` derived its logical file length solely from the OS-reported file size, so reads could extend past the last committed chunk boundary. Similarly, `CompressedSequentialWriter.open()` used a single `finishType` flag that conflated "final write" with "shared early view", making it impossible to independently control whether to resize the offset array and whether to retain a reference. The combination caused range queries to include a one-key overlap between the early-opened file and the still-compacting source SSTables.

### Before / After

```java
// BEFORE — RandomAccessReader used real channel size, ignoring the logical limit
public static RandomAccessReader open(File file, PoolingSegmentedFile owner) {
    return open(file, DEFAULT_BUFFER_SIZE, owner);
}
// constructor derived fileLength from channel.size() unconditionally
```

```java
// AFTER — callers pass an explicit overrideLength so the reader stops at the correct boundary
protected RandomAccessReader(File file, int bufferSize, long overrideLength, PoolingSegmentedFile owner) {
    ...
    long fileLength = overrideLength;
    if (fileLength <= 0)
        fileLength = channel.size();
    this.fileLength = fileLength;
}
```

```java
// BEFORE — compressed range scanned one key past the end for EARLY-opened readers
long right = (rightBound.compareTo(last) > 0)
    ? (openReason == OpenReason.EARLY
        ? getPosition(last, Operator.GT, false, true).position
        : uncompressedLength())
    : getPosition(rightBound, Operator.GT).position;

// AFTER — always use uncompressedLength(); SSTableReader.last is now exact
long right = (rightBound.compareTo(last) > 0)
    ? uncompressedLength()
    : getPosition(rightBound, Operator.GT).position;
```

---

## How to Spot

When a logical "file length" is derived from the OS file size rather than from application-level metadata, it will be wrong for files that are still being written or that have a known committed length. Look for any `channel.size()` call in a reader whose owner tracks a separate `length` field, and verify that the reader respects that field rather than the raw channel size.

---

## Test

No test added.
