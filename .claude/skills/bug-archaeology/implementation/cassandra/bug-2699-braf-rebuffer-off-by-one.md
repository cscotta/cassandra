# Bug 2699: Excessive rebuffering in BufferedRandomAccessFile on exact-boundary seek

**Commit**: 28414b198c · **Ticket**: CASSANDRA-2581

## Detection Rule
When checking whether a new file position falls within the current buffer, use a strict `>` comparison against `bufferOffset + validBufferBytes` rather than `>=`, because the byte at `bufferOffset + validBufferBytes` is the first byte of the *next* buffer (not part of the current one) only if it was never written; a position exactly equal to `bufferOffset + validBufferBytes` is still in-bounds during a write.

## Code Smell
`if (newPosition >= bufferOffset + validBufferBytes || newPosition < bufferOffset)` causes an unnecessary `reBuffer()` call when seeking to exactly the end of the current valid buffer region, discarding data that could be appended in place.

## Tags
- scope: java
- category: off-by-one
- code-pattern: inclusive-upper-bound-buffer-check
- severity: performance
- subsystem: storage-engine

## Root Cause
`BufferedRandomAccessFile.seek()` called `reBuffer()` when `newPosition >= bufferOffset + validBufferBytes`. This triggered unnecessary disk I/O and buffer invalidation on appending writes where the position lands precisely at the end of the current buffer. Changing to `>` allows that position to be served from the existing buffer, significantly improving flush and compaction throughput.

## Example
```java
// BEFORE
if (newPosition >= bufferOffset + validBufferBytes || newPosition < bufferOffset)
    reBuffer();
```
```java
// AFTER
if (newPosition > (bufferOffset + validBufferBytes) || newPosition < bufferOffset)
    reBuffer();
```
