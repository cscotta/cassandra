# Bug 0036: ORC VectorizedRowBatchIterator not registered with CloseableGroup, leaking file handles

**Commit**: f475ccb5c579b8a1d4842507b12e854161dea7af · **Ticket**: #16086

## Detection Rule

When a resource is created inside a method of a `CloseableGroup` subclass and later wrapped or returned, verify it is registered via `addCloseable()` before the wrapping happens, so the group's own `close()` path releases it even if the caller never closes the wrapper.

## Code Smell

A `CloseableGroup` subclass creates a `Closeable` object, passes it into a wrapping iterator or reader, but never calls `addCloseable()` on it; the wrapper is returned directly to the caller with no guarantee the inner resource is tracked.

## Tags

- scope: universal
- category: resource-leak
- code-pattern: close-not-on-all-paths
- severity: crash
- subsystem: orc

## Root Cause

`OrcIterable.iterator()` created a `VectorizedRowBatchIterator` (which holds an ORC `RecordReader` and its underlying input stream) and wrapped it in a `CloseableIterator.transform()`. The `VectorizedRowBatchIterator` itself was never added to the enclosing `CloseableGroup`, so calling `close()` on the `OrcIterable` did not close the inner iterator or its `RecordReader`. Only adding `addCloseable(rowBatchIterator)` before the wrapping ensured the file handle was released when the iterable was closed.

## Example

```java
// BEFORE
VectorizedRowBatchIterator rowBatchIterator =
    newOrcIterator(file, readOrcSchema, start, length, orcFileReader, sarg, recordsPerBatch);
if (batchReaderFunction != null) {
  OrcBatchReader<T> batchReader = ...;
  return CloseableIterator.transform(rowBatchIterator, batchReader::read);
}
```

```java
// AFTER
VectorizedRowBatchIterator rowBatchIterator =
    newOrcIterator(file, readOrcSchema, start, length, orcFileReader, sarg, recordsPerBatch);
addCloseable(rowBatchIterator);  // ensures close() on the group also closes the iterator
if (batchReaderFunction != null) {
  OrcBatchReader<T> batchReader = ...;
  return CloseableIterator.transform(rowBatchIterator, batchReader::read);
}
```
