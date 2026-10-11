# Bug 1464: Fix Wrong Progress When Streaming Uncompressed

**Commit**: 3db38d7ed1 · **Ticket**: CASSANDRA-7878
**Subsystem**: src/java/org/apache/cassandra/streaming/StreamWriter.java
**Category**: Off-by-one

---

## Root Cause

When streaming uncompressed SSTables the `write()` helper reads a chunk that includes a leading alignment offset (`start` / `readOffset`) for checksum validation, then skips that prefix before writing to the network. Progress was being incremented by the full number of bytes returned from `write()` including the skipped prefix, causing the reported bytes-sent to exceed the actual bytes transferred. Additionally, the rate limiter was acquiring tokens for the full chunk including the skipped prefix.

### Before / After

```java
// BEFORE
long lastWrite = write(file, validator, skipBytes, length, bytesTransferred);
bytesTransferred += lastWrite;
progress += lastWrite;              // includes the skipped prefix bytes
...
limiter.acquire(toTransfer);        // acquires too many tokens
```

```java
// AFTER
long lastBytesRead = write(file, validator, readOffset, length, bytesRead);
bytesRead += lastBytesRead;
progress += (lastBytesRead - readOffset);  // subtract the skipped prefix
...
limiter.acquire(toTransfer - start);       // only tokens for bytes actually sent
```

---

## How to Spot

Any time a function returns "bytes read from file" but the caller uses that value directly as "bytes sent over the network," check whether the read window is wider than the transfer window (e.g., for alignment or validation overlap). The two counts are only equal when the read offset is zero.

---

## Test

No test added.
