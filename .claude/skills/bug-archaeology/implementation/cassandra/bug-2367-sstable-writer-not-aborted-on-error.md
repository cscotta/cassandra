# Bug 2367: SSTableWriter not aborted on IOException, leaving partial SSTable files for bulk loader

**Commit**: 10a64bd681 · **Ticket**: CASSANDRA-3946

## Detection Rule

When an `SSTableWriter` is created inside a method that can throw `IOException`, verify that a `try/catch` calls `writer.abort()` on failure — without this, a partially written SSTable is left on disk and may be streamed as a valid (but corrupt) file.

## Code Smell

`SSTableWriter writer = getWriter(); writer.closeAndOpenReader();` without a surrounding try/catch means any `IOException` during write leaves the writer open and its incomplete files on disk, which the bulk loader will then attempt to stream to the cluster.

## Tags

- scope: cassandra
- category: resource-leak
- code-pattern: writer-not-aborted-on-error
- severity: data-loss
- subsystem: tools-clients

## Root Cause

`SSTableSimpleUnsortedWriter.sort()` and `SSTableSimpleWriter.close()` created an `SSTableWriter` and wrote rows without protecting the operation with a try/catch. If an `IOException` was thrown partway through (e.g., disk full), the writer was never aborted, leaving an incomplete SSTable on disk. The bulk loader would subsequently stream this partial file to the cluster, potentially corrupting data. The fix wraps the write path in try/catch and calls `writer.abort()` on error before re-throwing.

Additionally, `getFilename()` used `false` for the `temporary` Descriptor flag, meaning the file was not marked as temporary during writing — changed to `true` to allow cleanup on error.

## Example

```java
// BEFORE
private void sort() throws IOException {
    SSTableWriter writer = getWriter();
    for (Map.Entry<...> entry : keys.entrySet())
        writer.append(entry.getKey(), entry.getValue());
    writer.closeAndOpenReader(); // IOException here leaves partial file on disk
}
```

```java
// AFTER
private void sort() throws IOException {
    SSTableWriter writer = null;
    try {
        writer = getWriter();
        for (Map.Entry<...> entry : keys.entrySet())
            writer.append(entry.getKey(), entry.getValue());
        writer.closeAndOpenReader();
    } catch (IOException e) {
        if (writer != null)
            writer.abort();
        throw e;
    }
}
```
