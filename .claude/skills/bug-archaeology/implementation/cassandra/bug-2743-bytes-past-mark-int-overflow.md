# Bug 2743: Hinted-handoff delivery fails with UnsupportedOperationException for column indexes > 2 GB

**Commit**: a4b71120ff · **Ticket**: CASSANDRA-2297

## Detection Rule
When tracking byte offsets in file structures that may exceed 2 GB, use `long` return types for byte-count methods, not `int`.

## Code Smell
`BufferedRandomAccessFile.bytesPastMark()` returns `int` and throws `UnsupportedOperationException("Overflow")` when the measured span exceeds `Integer.MAX_VALUE`, which prevents hinted-handoff delivery for any row whose column index block is larger than 2 GB.

## Tags
- scope: cassandra
- category: wrong-constant
- code-pattern: int-overflow-file-offset
- severity: crash
- subsystem: storage-engine

## Root Cause
`bytesPastMark()` and `bytesPastMark(FileMark)` computed the span between two file positions as a `long` internally, then checked whether it exceeded `Integer.MAX_VALUE` and threw if so, otherwise cast to `int`. Callers that needed to track column-index positions larger than 2 GB (possible for wide rows) received this exception instead of the correct offset. The fix changes the return type to `long` in `BufferedRandomAccessFile`, `MappedFileDataInput`, and the `FileDataInput` interface, removing the overflow check and cast entirely.

## Example
```java
// BEFORE — throws UnsupportedOperationException for spans > 2 GB
public int bytesPastMark() {
    long bytes = getFilePointer() - markedPointer;
    assert bytes >= 0;
    if (bytes > Integer.MAX_VALUE)
        throw new UnsupportedOperationException("Overflow: " + bytes);
    return (int) bytes;
}
```
```java
// AFTER — returns full long span without overflow check
public long bytesPastMark() {
    long bytes = getFilePointer() - markedPointer;
    assert bytes >= 0;
    return bytes;
}
```
