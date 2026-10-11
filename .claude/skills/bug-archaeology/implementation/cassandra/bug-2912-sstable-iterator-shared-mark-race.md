# Bug 2912: Race condition in SSTable iterators due to shared mark state

**Commit**: 5531a7f5db · **Ticket**: CASSANDRA-1130

## Detection Rule
When a file object holds a single shared `markedPointer` field, any concurrent or interleaved call to `mark()` on the same object overwrites the previous mark, causing `reset()` and `bytesPastMark()` to use a stale position.

## Code Smell
Classes that store a single `long markedPointer` field on the file object and expose `mark()`/`reset()` without returning a position token; two callers marking the same file will silently clobber each other's marks.

## Tags
- scope: cassandra
- category: race-condition
- code-pattern: mutable-shared-mark-state
- severity: silent-wrong-result
- subsystem: storage-engine

## Root Cause
`BufferedRandomAccessFile` kept a single `markedPointer` field. When `SSTableNamesIterator` and `SSTableSliceIterator` called `file.mark()` at different times, the second call overwrote the first. Subsequent `reset()` and `bytesPastMark()` calls used the wrong position, causing columns to be read from incorrect offsets or loops to terminate early. The fix introduces a `FileMark` value object returned by `mark()` so each call site holds its own independent mark.

## Example
```java
// BEFORE: shared mutable state on the file
private long markedPointer;
public void mark()        { markedPointer = getFilePointer(); }
public void reset()       { seek(markedPointer); }
public int bytesPastMark(){ return (int)(getFilePointer() - markedPointer); }
// Two callers using same file: second mark() clobbers first
```
```java
// AFTER: mark returns an immutable token
public FileMark mark() { return new BufferedRandomAccessFileMark(getFilePointer()); }
public void reset(FileMark mark) { seek(((BufferedRandomAccessFileMark)mark).pointer); }
public int bytesPastMark(FileMark mark) {
    return (int)(getFilePointer() - ((BufferedRandomAccessFileMark)mark).pointer);
}
```
