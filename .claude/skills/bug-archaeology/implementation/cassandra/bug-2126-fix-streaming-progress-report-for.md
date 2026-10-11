# Bug 2126: Streaming Progress for Compressed Files Reported Decompressed Bytes Instead of Compressed Bytes

**Commit**: a548f05dea · **Ticket**: CASSANDRA-5130
**Subsystem**: src/java/org/apache/cassandra/streaming/IncomingStreamReader.java, src/java/org/apache/cassandra/streaming/compress/CompressedInputStream.java
**Category**: Wrong constant / default value

---

## Root Cause

`IncomingStreamReader` tracks streaming progress by incrementing `remoteFile.progress`. For compressed transfers, `remoteFile.size` represents the total compressed bytes on the wire. However, the progress field was being incremented with decompressed (uncompressed) byte counts from `CompressedInputStream.uncompressedBytes()`. Because decompressed data is typically larger than compressed data, progress could appear to exceed 100% or otherwise be misleading. The fix changes the metric tracked in `CompressedInputStream` from cumulative uncompressed bytes to cumulative compressed bytes read, renames the field/method accordingly (`totalCompressedBytesRead` / `getTotalCompressedBytesRead()`), and uses an absolute assignment (`remoteFile.progress = ...`) rather than an increment so that the value stays consistent with the stream's own accounting.

### Before / After

```java
// BEFORE — progress incremented with decompressed byte count
remoteFile.progress += remoteFile.compressionInfo != null
                       ? ((CompressedInputStream) underliningStream).uncompressedBytes()
                       : in.getBytesRead();
// In CompressedInputStream: uncompressedBytes += validBufferBytes;
```

```java
// AFTER — progress set to total compressed bytes read
if (remoteFile.compressionInfo != null)
    remoteFile.progress = ((CompressedInputStream) underliningStream).getTotalCompressedBytesRead();
else
    remoteFile.progress += in.getBytesRead();
// In CompressedInputStream: totalCompressedBytesRead += compressed.length;
```

---

## How to Spot

When reporting progress against a "size" denominator, verify that the numerator (bytes counted) and denominator (total expected bytes) measure the same quantity — both compressed or both uncompressed. A mismatch between the two is a silent correctness bug that only shows up as wrong percentages at runtime.

---

## Test

No test added.
