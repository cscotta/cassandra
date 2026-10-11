# Bug 1315: Fix Adler32 Digest for Compressed SSTables

**Commit**: 0e621317ed · **Ticket**: CASSANDRA-8778
**Subsystem**: src/java/org/apache/cassandra/io/util/DataIntegrityMetadata.java
**Category**: Wrong serialization / deserialization

---

## Root Cause

`DataIntegrityMetadata.ChecksumWriter#append()` computed a full-file Adler32 checksum by hashing only the raw data bytes, but for compressed SSTables each chunk's per-chunk checksum integer is written inline with the compressed data. Any external tool (or verification pass) computing the file checksum would include those inline checksum bytes, so the stored full checksum would never match. The fix adds a `checksumIncrementalResult` boolean parameter: when true (compressed path), the 4-byte chunk checksum value is also fed into the full-file checksum accumulator, keeping the two computations consistent.

### Before / After

```java
// BEFORE
public void append(byte[] buffer, int start, int end)
{
    incrementalChecksum.update(buffer, start, end);
    incrementalOut.writeInt((int) incrementalChecksum.getValue());
    incrementalChecksum.reset();
    fullChecksum.update(buffer, start, end);
}
```

```java
// AFTER
public void append(byte[] buffer, int start, int end, boolean checksumIncrementalResult)
{
    incrementalChecksum.update(buffer, start, end);
    incrementalChecksumValue = (int) incrementalChecksum.getValue();
    incrementalOut.writeInt(incrementalChecksumValue);
    incrementalChecksum.reset();
    fullChecksum.update(buffer, start, end);
    if (checksumIncrementalResult)
    {
        ByteBuffer byteBuffer = ByteBuffer.allocate(4);
        byteBuffer.putInt(incrementalChecksumValue);
        fullChecksum.update(byteBuffer.array(), 0, byteBuffer.array().length);
    }
}
```

---

## How to Spot

When a file format interleaves metadata (such as per-chunk checksums) with data bytes, any rolling checksum over the file must account for those metadata bytes. Reviewers should trace every byte written to the file and verify the checksum accumulator sees exactly the same sequence. A shared `append()` method used by both compressed and uncompressed writers with a single code path is a warning sign that the two cases may have different serialized layouts.

---

## Test

No test added.
