# Bug 1717: Fix Leaking Validator File Handle in StreamWriter

**Commit**: da2d97142b · **Ticket**: CASSANDRA-6832
**Subsystem**: streaming/StreamWriter
**Category**: Resource leak

---

## Root Cause

`StreamWriter.write()` opened a `ChecksumValidator` for the SSTable's CRC component when it existed, but the `finally` block only closed the `RandomAccessReader` for the data file. If an exception was thrown during streaming, the `ChecksumValidator` (which holds its own file handle) was never closed, leaking the file descriptor. The fix adds `FileUtils.closeQuietly(validator)` to the `finally` block.

### Before / After

```java
// BEFORE
finally
{
    FileUtils.closeQuietly(file);
}
```

```java
// AFTER
finally
{
    FileUtils.closeQuietly(file);
    FileUtils.closeQuietly(validator);
}
```

---

## How to Spot

Whenever multiple `Closeable` resources are opened in a method, each one must appear in the `finally` block (or a try-with-resources). A common oversight is adding a second resource after the first and forgetting to add a matching close call.

---

## Test

No test added.
