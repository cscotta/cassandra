# Bug 0639: Decompression Uses chunkLength Instead of maxCompressedLength for Compression Decision

**Commit**: d080a73723 · **Ticket**: CASSANDRA-13899
**Subsystem**: `streaming/compress/CompressedInputStream.java`
**Category**: Logic error in condition

---

## Root Cause

`CompressedInputStream.decompress()` decided whether a received buffer was compressed by checking `length - CHECKSUM_LENGTH < compressedChunkLength` (the uncompressed chunk size). CASSANDRA-10520 had established that the correct sentinel is `maxCompressedLength`: a buffer at or above the maximum possible compressed size is assumed to be uncompressed. Using the uncompressed `chunkLength` as the threshold was wrong — a legitimately compressed buffer whose compressed size happened to be ≥ `chunkLength` would be misidentified as uncompressed and passed to the caller without decompression, silently corrupting streamed data.

### Before / After

```java
// BEFORE — compares against uncompressed chunk size
final int compressedChunkLength = info.parameters.chunkLength();
int length = compressed.remaining();
if (length - CHECKSUM_LENGTH < compressedChunkLength)
{
    // decompress
}
// else treat as uncompressed
```

```java
// AFTER — compares against maximum possible compressed length
int length = compressed.remaining();
if (length - CHECKSUM_LENGTH < info.parameters.maxCompressedLength())
{
    // decompress
}
// else treat as uncompressed
// compressedChunkLength still used below for buffer alignment
final int compressedChunkLength = info.parameters.chunkLength();
bufferOffset = current & ~(compressedChunkLength - 1);
```

---

## How to Spot

Whenever a compressed-vs-uncompressed decision uses a size threshold, the threshold must come from the compressor's maximum output size for a given input chunk, not the input chunk size itself. Mixing these two values is easy because both are related to "chunk length" in the API; reviewers should confirm which `chunkLength` method is appropriate for boundary detection vs. buffer alignment.

---

## Test

No test added.
