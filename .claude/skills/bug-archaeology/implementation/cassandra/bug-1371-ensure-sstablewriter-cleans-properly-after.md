# Bug 1371: SSTableWriter Does Not Clean Up Properly After Write Failure

**Commit**: 3679b1bd27 · **Ticket**: CASSANDRA-8499
**Subsystem**: src/java/org/apache/cassandra/io/sstable/SSTableWriter.java
**Category**: State not cleaned up

---

## Root Cause

`SSTableWriter.abort()` called `FileUtils.closeQuietly()` on the index writer and data file, but `closeQuietly` is a normal close that flushes and finalises the file—not a best-effort teardown. More critically, the `CompressedSequentialWriter` did not override `abort()`, so calling `abort()` on a compressed data file fell back to the base `SequentialWriter.close()` path, which could throw, leaving the metadata writer and directory file descriptor unclosed. Additionally, when an exception was thrown during `SSTableWriter.append()`, the caller received an `FSWriteError` but the in-progress `SequentialWriter` was never aborted, leaking its OS resources.

### Before / After

```java
// BEFORE: SSTableWriter.abort()
public void abort()
{
    assert descriptor.temporary;
    FileUtils.closeQuietly(iwriter);   // normal close; can throw; no abort semantics
    FileUtils.closeQuietly(dataFile);
    ...
}
```

```java
// AFTER: SSTableWriter.abort()
public void abort()
{
    assert descriptor.temporary;
    iwriter.abort();    // dedicated abort path; swallows exceptions
    dataFile.abort();
    ...
}
```

---

## How to Spot

Any class that wraps one or more `Closeable` resources and exposes an `abort()` method must override `abort()` in every subclass that adds new resources. If a subclass adds a resource (e.g., a metadata writer) but only inherits the parent's `abort()`, that resource leaks on failure paths. A useful audit heuristic: for each field that is a `Closeable`, verify there is a corresponding `abort()` call in the class's own `abort()` override, not just in `close()`.

---

## Test

No test added.
