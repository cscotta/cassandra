# Bug 0336: SSTable Component Resource Leak on Exception During Open

**Commit**: 8e28dc0eba · **Ticket**: CASSANDRA-17174
**Subsystem**: src/java/org/apache/cassandra/io/sstable/format/big/BigTableWriter.java
**Category**: Resource leak

---

## Root Cause

In `BigTableWriter.openEarly()` and `openFinalEarlyReader()`, the code allocated an `IndexSummary`, two `FileHandle` objects (`ifile`, `dfile`), and an `SSTableReader` in sequence with no exception handling. If any step after the first allocation threw (e.g., during `FileHandle.complete()` or `SSTableReader.internalOpen()`), the already-allocated resources were never released, causing file descriptor and off-heap memory leaks. The fix wraps each open path in a `try/catch`, and if the `SSTableReader` was not yet constructed, closes `indexSummary`, `ifile`, and `dfile` individually; if the `SSTableReader` was constructed, it delegates cleanup to its `InstanceTidier` via `selfRef().release()`.

### Before / After

```java
// BEFORE (no exception handling)
IndexSummary indexSummary = iwriter.summary.build(metadata().partitioner, boundary);
FileHandle ifile = iwriter.builder.bufferSize(indexBufferSize).complete(boundary.indexLength);
FileHandle dfile = dbuilder.bufferSize(dataBufferSize).complete(boundary.dataLength);
SSTableReader sstable = SSTableReader.internalOpen(...);
return sstable;
```

```java
// AFTER
IndexSummary indexSummary = null;
FileHandle ifile = null;
FileHandle dfile = null;
SSTableReader sstable = null;
try {
    indexSummary = iwriter.summary.build(metadata().partitioner, boundary);
    ifile = iwriter.builder.bufferSize(indexBufferSize).complete(boundary.indexLength);
    dfile = dbuilder.bufferSize(dataBufferSize).complete(boundary.dataLength);
    sstable = SSTableReader.internalOpen(...);
    return sstable;
} catch (Throwable t) {
    JVMStabilityInspector.inspectThrowable(t);
    if (sstable != null)
        sstable.selfRef().release();
    else
        Stream.of(indexSummary, ifile, dfile).filter(Objects::nonNull).forEach(SharedCloseableImpl::close);
    throw t;
}
```

---

## How to Spot

Whenever a method acquires multiple `AutoCloseable` or ref-counted resources in sequence, review whether a failure midway through leaves earlier resources uncleaned. The pattern of allocating `A`, then `B`, then `C` with a single `return C` at the end is a classic "stepwise leak" — each step that follows the first can throw, and without a `try/catch` or try-with-resources chain the earlier allocations are abandoned. In Cassandra's SSTable layer, `IndexSummary` and `FileHandle` are reference-counted (`SharedCloseableImpl`) rather than `AutoCloseable`, so try-with-resources is not directly applicable; an explicit `catch (Throwable t)` block with null-guarded cleanup is the required pattern.

---

## Test

No new test added; the fix is defensive hardening of an existing code path against future exceptions.
