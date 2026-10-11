# Bug 2901: SSTableIdentityIterator hasNext() uses file pointer instead of tracker bytes

**Commit**: 0fe7942804 · **Ticket**: CASSANDRA-2901

## Detection Rule
When an iterator tracks its position using a `BytesReadTracker` wrapping a `DataInput`, use `inputWithTracker.getBytesRead()` for the `hasNext()` bound rather than `file.getFilePointer()`, because the file pointer and the tracker can diverge after seeks.

## Code Smell
`file.getFilePointer() < finishedAt` used as the `hasNext()` guard in a class that also reads through `inputWithTracker` will get out of sync whenever the tracker is reset or the file is re-seeked, causing premature termination or unbounded iteration.

## Tags
- scope: cassandra
- category: logic-error
- code-pattern: dual-position-tracking-out-of-sync
- severity: silent-wrong-result
- subsystem: storage-engine

## Root Cause
`SSTableIdentityIterator` maintained two position sources: `file.getFilePointer()` (the raw file position) and `inputWithTracker.getBytesRead()` (bytes consumed through the tracker). After a `checkData` validation pass that seeked the file back to `dataStart`, the file pointer was reset but the tracker was not — or vice versa — causing the two to diverge. The fix removes `finishedAt` and all `instanceof RandomAccessReader` branches, using only `inputWithTracker.getBytesRead() < dataSize` throughout.

## Example
```java
// BEFORE
public boolean hasNext() {
    if (input instanceof RandomAccessReader) {
        RandomAccessReader file = (RandomAccessReader) input;
        return file.getFilePointer() < finishedAt;
    } else {
        return inputWithTracker.getBytesRead() < dataSize;
    }
}
```
```java
// AFTER
public boolean hasNext() {
    return inputWithTracker.getBytesRead() < dataSize;
}
```
