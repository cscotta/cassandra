# Bug 2619: BytesReadTracker.readChar increments count by 1 instead of 2, skipBytes uses n instead of skipped

**Commit**: c024da47b4 · **Ticket**: n/a

## Detection Rule
When implementing a byte-counting wrapper around `DataInput`, ensure each `readXxx()` method increments the counter by the actual byte width of the type, and `skipBytes` increments by the actually-skipped count rather than the requested count.

## Code Smell
`bytesRead += 1` after `source.readChar()` (which reads 2 bytes) and `bytesRead += n` after `source.skipBytes(n)` (which may skip fewer than `n` bytes) both corrupt the read-position counter.

## Tags
- scope: cassandra
- category: off-by-one
- code-pattern: byte-counter-wrong-increment
- severity: silent-wrong-result
- subsystem: storage-engine

## Root Cause
`BytesReadTracker` is used by `SSTableIdentityIterator` to track how many bytes have been consumed from a `DataInput`. Two methods were incorrect: `readChar()` added 1 instead of 2 (a `char` is 2 bytes in Java), and `skipBytes()` added the requested skip `n` rather than the actually-skipped return value, which may be less than `n` if the stream ends early. Both errors cause `getBytesRead()` to return wrong values, which in turn mis-positions the iterator.

## Example
```java
// BEFORE
public char readChar() throws IOException {
    char c = source.readChar();
    bytesRead += 1;  // wrong: char is 2 bytes
    return c;
}
public int skipBytes(int n) throws IOException {
    int skipped = source.skipBytes(n);
    bytesRead += n;  // wrong: should be skipped
    return skipped;
}
```
```java
// AFTER
public char readChar() throws IOException {
    char c = source.readChar();
    bytesRead += 2;
    return c;
}
public int skipBytes(int n) throws IOException {
    int skipped = source.skipBytes(n);
    bytesRead += skipped;
    return skipped;
}
```
