# Bug 3033: CompressedSequentialWriter uses read() instead of readFully() for chunk data

**Commit**: 5ab428ae06 · **Ticket**: CASSANDRA-3033

## Detection Rule
When reading an exact number of bytes from a stream or file into a buffer, use `readFully()` rather than `read()` because `read()` may return fewer bytes than requested without signalling an error.

## Code Smell
`out.read(compressed, 0, chunkSize)` returns the number of bytes actually read (which may be less than `chunkSize`) without any retry loop, silently leaving the rest of the buffer uninitialised.

## Tags
- scope: cassandra
- category: logic-error
- code-pattern: read-instead-of-readfully
- severity: silent-wrong-result
- subsystem: storage-engine

## Root Cause
`CompressedSequentialWriter.resetAndTruncate()` sought to a chunk offset and called `out.read(compressed, 0, chunkSize)` to read the compressed chunk for re-use. `read()` is not guaranteed to fill the buffer, so partial reads would pass truncated data to Snappy for decompression, producing corrupt output. The fix replaces `read` with `readFully`.

## Example
```java
// BEFORE
out.seek(chunkOffset);
out.read(compressed, 0, chunkSize);
int validBytes = Snappy.rawUncompress(compressed, 0, chunkSize, buffer, 0);
```
```java
// AFTER
out.seek(chunkOffset);
out.readFully(compressed, 0, chunkSize);
int validBytes = Snappy.rawUncompress(compressed, 0, chunkSize, buffer, 0);
```
