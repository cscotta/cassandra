# Bug 2764: BufferedRandomAccessFile Returns -1 When Exactly at Last Byte

**Commit**: 92d5315bb4 · **Ticket**: CASSANDRA-2213

## Detection Rule
When checking whether a buffer read should return -1 for EOF, use `length > bufferEnd` (strictly greater) rather than `length >= bufferEnd`, otherwise reading the last valid byte in the buffer is falsely treated as EOF.

## Code Smell
`if (length >= bufferEnd && hitEOF) return -1;` returns -1 when `length == bufferEnd`, but `bufferEnd` is the exclusive upper bound, so the byte at `bufferEnd - 1` is still valid.

## Tags
- scope: cassandra
- category: off-by-one
- code-pattern: inclusive-vs-exclusive-bound
- severity: crash
- subsystem: storage-engine

## Root Cause
`readAtMost` in `BufferedRandomAccessFile` guarded against reading past EOF with `length >= bufferEnd && hitEOF`. Since `bufferEnd` is the exclusive limit (the first position *not* in the buffer), a request positioned at exactly `bufferEnd` should return -1, but a request for `bufferEnd - 1` bytes is valid. The `>=` caused a premature -1 for the last byte of a file, which `readInt`/`readFully` escalated to an `EOFException`. The fix changes the comparison to `>`.

## Example
```java
// BEFORE
if (length >= bufferEnd && hitEOF)
    return -1;
```
```java
// AFTER
if (length > bufferEnd && hitEOF)
    return -1;
```
