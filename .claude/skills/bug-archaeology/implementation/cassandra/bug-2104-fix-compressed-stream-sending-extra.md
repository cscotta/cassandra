# Bug 2104: Compressed Streaming Sends Extra Chunk at Section Boundary

**Commit**: ee0be0624b · **Ticket**: CASSANDRA-5105
**Subsystem**: src/java/org/apache/cassandra/io/compress/CompressionMetadata.java
**Category**: Off-by-one

---

## Root Cause

When computing which compressed chunks to include for a streaming section, `getChunksForSections` calculated the end chunk index as `section.right / chunkLength`. If `section.right` fell exactly on a chunk boundary (i.e. `section.right % chunkLength == 0`), the division produced an index one past the last chunk that actually contains any data for the section, causing one extra unnecessary chunk to be sent.

The fix adjusts `endIndex` down by one whenever the right endpoint lands exactly on a boundary, so only chunks that genuinely overlap the requested section are included.

### Before / After

```java
// BEFORE
int startIndex = (int) (section.left / parameters.chunkLength());
int endIndex   = (int) (section.right / parameters.chunkLength());
for (int i = startIndex; i <= endIndex; i++)
```

```java
// AFTER
int startIndex = (int) (section.left / parameters.chunkLength());
int endIndex   = (int) (section.right / parameters.chunkLength());
endIndex = section.right % parameters.chunkLength() == 0 ? endIndex - 1 : endIndex;
for (int i = startIndex; i <= endIndex; i++)
```

---

## How to Spot

When dividing an offset range into fixed-size buckets, check both endpoints: an endpoint that equals an exact multiple of the bucket size does not actually require the next bucket. Always validate inclusive vs. exclusive range semantics and add a boundary test at exact multiples.

---

## Test

A new `CompressedInputStreamTest` was added that writes 1000 longs to a compressed SSTable file and then reads back specific values by streaming only their sections, including values whose byte positions fall exactly on chunk boundaries, verifying no extra data is read.
