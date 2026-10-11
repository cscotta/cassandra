# Bug 2409: BulkRecordWriter NPE when Hadoop reducer produces no output

**Commit**: a47d4cbf0f · **Ticket**: CASSANDRA-3944

## Detection Rule

When a resource (writer, connection, stream) is conditionally initialized only if data is present, its `close()` / `flush()` code must check for null before using it.

## Code Smell

`writer.close()` and `loader.stream().get()` called unconditionally in `close()`, but `writer` is only assigned when the `write()` method is called — a reducer with no input data leaves `writer` null.

## Tags

- scope: cassandra
- category: missing-null-check
- code-pattern: lazy-init-resource-unconditional-close
- severity: crash
- subsystem: tools-clients

## Root Cause

`BulkRecordWriter` lazily initializes the SSTable `writer` when the first mutation is written. The `close()` method, called by Hadoop at the end of the reduce phase, called `writer.close()` and `loader.stream().get()` unconditionally. When a reducer received no input rows, `writer` was never initialized (remained null), causing a NPE in `close()`. The fix wraps the close logic in `if (writer != null)`.

## Example

```java
// BEFORE — NPE if no data was written
private void close() throws IOException {
    writer.close();          // NPE when writer == null
    loader.stream().get();
}
```

```java
// AFTER
private void close() throws IOException {
    if (writer != null) {
        writer.close();
        loader.stream().get();
    }
}
```
