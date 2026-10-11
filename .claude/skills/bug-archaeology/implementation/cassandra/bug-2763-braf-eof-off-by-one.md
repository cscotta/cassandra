# Bug 2763: BRAF Seeks Past EOF Trigger Unnecessary Buffer Reload

**Commit**: 5e8b0d68cb · **Ticket**: CASSANDRA-2218

## Detection Rule
When checking whether a seek position is at or past end-of-file, use `>= channel.size()` instead of `> channel.size()` to avoid attempting a buffer fill from the exact EOF position.

## Code Smell
`if (bufferOffset > channel.size())` misses the case where `bufferOffset == channel.size()`, causing `reBuffer()` to issue a channel read at the exact EOF position which returns 0 bytes but still incurs system-call overhead.

## Tags
- scope: cassandra
- category: off-by-one
- code-pattern: strict-greater-than-instead-of-gte
- severity: performance
- subsystem: storage-engine

## Root Cause
`BufferedRandomAccessFile.reBuffer()` checked `bufferOffset > channel.size()` before deciding to skip re-reading. When `bufferOffset == channel.size()` (i.e., positioned exactly at EOF), the check was false and the code proceeded to read zero bytes from the channel, wasting a syscall and setting confusing state. The fix changes to `bufferOffset >= channel.size()`.

## Example
```java
// BEFORE
if (bufferOffset > channel.size()) {
    buffer.rewind();
    bufferEnd = bufferOffset;
    hitEOF = true;
    return 0;
}
```
```java
// AFTER
if (bufferOffset >= channel.size()) {
    buffer.rewind();
    bufferEnd = bufferOffset;
    hitEOF = true;
    return 0;
}
```
