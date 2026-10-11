# Bug 2972: NPE when closing SSTableSliceIterator that found no data

**Commit**: 939709f977 · **Ticket**: CASSANDRA-341

## Detection Rule
When a resource (file handle, iterator, reader) is only created conditionally inside a constructor (e.g., `if (position >= 0) reader = new ...`), check whether the `close()` method guards against the resource being null.

## Code Smell
`SSTableSliceIterator.close()` calls `reader.close()` unconditionally, but `reader` is only assigned in the constructor when `position >= 0`; when the key is not found in the SSTable, `reader` remains null and `close()` throws `NullPointerException`.

## Tags
- scope: java
- category: missing-null-check
- code-pattern: unconditional-close-on-nullable-resource
- severity: crash
- subsystem: storage-engine

## Root Cause
The `SSTableSliceIterator` constructor sets `reader = new ColumnGroupReader(...)` only when `ssTable.getPosition(decoratedKey) >= 0` (key found). When the key is not in the SSTable, `reader` is null. The `close()` method did not check for this case, causing NPE any time a slice query found no matching key in an SSTable file. The fix adds `if (reader != null)` before calling `reader.close()`.

## Example
```java
// BEFORE: NPE when key not found in SSTable
public void close() throws IOException {
    reader.close();  // reader may be null
}
```
```java
// AFTER: null guard
public void close() throws IOException {
    if (reader != null)
        reader.close();
}
```
