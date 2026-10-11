# Bug 2918: File descriptor leak in SSTableSliceIterator when opening its own file

**Commit**: b39c578581 · **Ticket**: CASSANDRA-1178

## Detection Rule
When a class conditionally creates a resource (file handle) only when a parameter is null, but the `closeFileWhenDone` flag is never set in that branch, the file is never closed.

## Code Smell
`closeFileWhenDone` defaults to `false`; the branch that creates a new `FileDataInput` forgets to set it to `true`, so `close()` skips closing the file the iterator itself opened.

## Tags
- scope: cassandra
- category: resource-leak
- code-pattern: missing-ownership-flag-set
- severity: crash
- subsystem: storage-engine

## Root Cause
`SSTableSliceIterator` had two constructors: one that accepts an existing `FileDataInput` and one that creates its own. The `closeFileWhenDone` flag was intended to track whether the iterator is responsible for closing the file, but the code path that actually opened a new file never set the flag to `true`. The result was that every time the no-file constructor was used, a file descriptor was leaked.

## Example
```java
// BEFORE
if (file == null)
{
    file = ssTable.getFileDataInput(decoratedKey, ...);
    // closeFileWhenDone never set — file never closed
    if (file == null) return;
}
```
```java
// AFTER
if (file == null)
{
    closeFileWhenDone = true;  // we created it, we close it
    file = ssTable.getFileDataInput(decoratedKey, ...);
    if (file == null) return;
}
```
