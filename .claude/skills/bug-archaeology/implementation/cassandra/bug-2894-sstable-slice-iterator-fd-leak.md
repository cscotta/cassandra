# Bug 2894: File descriptor leak in SSTableSliceIterator when key not found

**Commit**: 8beda131ba · **Ticket**: CASSANDRA-1416

## Detection Rule
When a constructor opens a file and returns early if the key is not found, check that `this.file` is assigned before the early return so the `close()` method has access to the handle; otherwise the handle opened before the return leaks.

## Code Smell
`SSTableSliceIterator` assigned `this.file = file` at the top of the constructor before the null check. When `file` was null (key not found), `this.file` was set to null before a new file handle was opened, and then an early `return` path failed to set `this.file` to the newly opened handle. The fd opened by `ssTable.getFileDataInput(...)` was then not referenced by the object and could not be closed.

## Tags
- scope: cassandra
- category: resource-leak
- code-pattern: file-handle-unassigned-before-early-return
- severity: performance
- subsystem: storage-engine

## Root Cause
The constructor removed the early `this.file = file` assignment at the top and instead assigns `this.file = file` only after the key lookup succeeds, ensuring the handle is always reachable via the field for subsequent `close()`. When the key is not found (null file returned by `getFileDataInput`), `this.file` is set to null explicitly so close is a no-op.

## Example
```java
// BEFORE — file set at top, then new handle opened, then early return without setting this.file
this.file = file;  // sets to null
if (file == null) {
    closeFileWhenDone = true;
    file = ssTable.getFileDataInput(...);  // opens new fd
    if (file == null)
        return;  // fd from getFileDataInput() leaked? No, but this.file still null
// ...
```
```java
// AFTER — this.file assigned only at the end when handle is valid
if (file == null) {
    closeFileWhenDone = true;
    file = ssTable.getFileDataInput(...);
    if (file == null) {
        this.file = null;
        return;
    }
    // ... key lookup ...
}
this.file = file;  // set once, after all early exits
```
