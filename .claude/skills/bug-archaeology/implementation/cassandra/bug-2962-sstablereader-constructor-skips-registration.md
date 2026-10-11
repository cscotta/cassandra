# Bug 2962: SSTableReader private constructor doesn't register in openedFiles map

**Commit**: 71b3b3c736 · **Ticket**: CASSANDRA-425

## Detection Rule
When a class has multiple constructors and one of them is the "canonical" one that performs registration (e.g., adding to a static map), verify that all other constructors also perform that registration or delegate to the canonical one.

## Code Smell
`SSTableReader` has a public `open(String, ...)` factory method that calls the public constructor (which adds to `openedFiles`) and a private `SSTableReader(String, IPartitioner)` constructor used during compaction that does NOT call `openedFiles.put()`, so later `get()` calls assert-fail with "No sstable opened for...".

## Tags
- scope: cassandra
- category: state-not-cleaned
- code-pattern: constructor-skips-registration
- severity: crash
- subsystem: compaction

## Root Cause
A refactoring (commit #413) split construction paths but forgot to add `openedFiles.put(filename, this)` to the private constructor. The public `open()` factory path registered the reader, but the compaction code path that used the private constructor produced unregistered `SSTableReader` instances. Any subsequent call to `SSTableReader.get(dataFileName)` for that file would trip the assertion `sstable != null`. The fix adds `openedFiles.put(filename, this)` to the private constructor.

## Example
```java
// BEFORE: private constructor doesn't register
private SSTableReader(String filename, IPartitioner partitioner) {
    super(filename, partitioner);
    // openedFiles.put() missing!
}
```
```java
// AFTER: all constructors register
private SSTableReader(String filename, IPartitioner partitioner) {
    super(filename, partitioner);
    openedFiles.put(filename, this);
}
```
