# Bug 2732: ByteBufferUtil.inputStream returns 0 instead of -1 at end-of-stream, causing infinite loop

**Commit**: 0d443b0376 · **Ticket**: CASSANDRA-2365

## Detection Rule
When implementing `InputStream.read(byte[], int, int)`, return `-1` when the buffer is exhausted before attempting to copy zero bytes.

## Code Smell
`read()` computes `len = Math.min(len, copy.remaining())` which becomes 0 at end-of-stream, then calls `copy.get(bytes, off, 0)` and returns 0 — a legal but incorrect value that signals "retry" to callers instead of "end of stream", causing infinite retry loops.

## Tags
- scope: cassandra
- category: logic-error
- code-pattern: missing-eof-sentinel
- severity: hang
- subsystem: storage-engine

## Root Cause
The anonymous `InputStream` returned by `ByteBufferUtil.inputStream()` did not check `copy.hasRemaining()` before computing the read length. When the underlying `ByteBuffer` was exhausted, `Math.min(len, 0)` produced 0, `get(bytes, off, 0)` was a no-op, and the method returned 0. The `InputStream` contract requires returning -1 at EOF; returning 0 caused callers (e.g., `IOUtils.readFully`) to loop indefinitely.

## Example
```java
// BEFORE — returns 0 at EOF, causing infinite loop in callers
public int read(byte[] bytes, int off, int len) throws IOException {
    len = Math.min(len, copy.remaining());
    copy.get(bytes, off, len);
    return len;
}
```
```java
// AFTER — returns -1 at EOF per InputStream contract
public int read(byte[] bytes, int off, int len) throws IOException {
    if (!copy.hasRemaining())
        return -1;
    len = Math.min(len, copy.remaining());
    copy.get(bytes, off, len);
    return len;
}
```
