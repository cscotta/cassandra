# Bug 2745: File Descriptor Leak in sstable2json with Non-MMap I/O

**Commit**: c7df15da53 · **Ticket**: CASSANDRA-2304

## Detection Rule
When a loop opens an `IColumnIterator` inside a try/catch but closes it only in the happy path, flag the missing `finally` block as a resource leak.

## Code Smell
An `IColumnIterator columns` is obtained from `filter.getSSTableColumnIterator(reader)` but `columns.close()` only appears in the success path; exceptions bypass the close call.

## Tags
- scope: cassandra
- category: resource-leak
- code-pattern: iterator-close-missing-finally
- severity: crash
- subsystem: tools-clients

## Root Cause
`SSTableExport.serializeSSTables` iterated columns twice: first to count them (to check the page boundary), then again to serialize. The iterator was not closed in a `finally` block, so file descriptors leaked when I/O used buffered (non-mmap) reads. The fix eliminates the double iteration by having `serializeRow` return the count, and wraps the iterator close in a `finally` block.

## Example
```java
// BEFORE
IColumnIterator columns = filter.getSSTableColumnIterator(reader);
// count columns without closing...
try {
    columns = filter.getSSTableColumnIterator(reader); // reset
    serializeRow(columns, isSuperCF, out);
} catch (IOException e) {
    System.err.println("WARNING: ...");
}
// columns never closed on exception
```
```java
// AFTER
IColumnIterator columns = filter.getSSTableColumnIterator(reader);
try {
    serialized = serializeRow(columns, isSuperCF, out);
} catch (IOException e) {
    System.err.println("WARNING: ..."); continue;
} finally {
    columns.close();
}
```
