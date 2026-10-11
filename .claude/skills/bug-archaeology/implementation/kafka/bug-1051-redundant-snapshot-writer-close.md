## Commit

bc797b077f — KAFKA-19314: Fix redundant close of SnapshotWriter in SnapshotEmitter

## Ticket

KAFKA-19314

## Subsystem

Metadata / KRaft snapshot

## Category

Resource leak

## Root cause

`SnapshotEmitter.maybeEmit()` held a reference to the raw `snapshotWriter` obtained from `RaftSnapshotWriter.get()` and closed it explicitly in the `finally` block via `Utils.closeQuietly(snapshotWriter.get(), "SnapshotWriter")`. However, `RaftSnapshotWriter.close()` already delegates to the same underlying snapshot writer. When `writer.close()` was called first, the second `closeQuietly` attempted to close an already-closed writer, potentially triggering erroneous state or spurious error logs. The fix removes the redundant `closeQuietly` call, relying solely on the outer `writer.close()`.

## Before / After

```java
// Before — redundant close of inner snapshotWriter
} finally {
    Utils.closeQuietly(snapshotWriter.get(), "SnapshotWriter");
    Utils.closeQuietly(writer, "RaftSnapshotWriter");
}

// After — single close through the wrapper
} finally {
    Utils.closeQuietly(writer, "RaftSnapshotWriter");
}
```

## How to spot

Look for `finally` blocks that close both an outer wrapper object and the inner resource it wraps. When the wrapper's `close()` already delegates to the inner resource, retaining the inner `closeQuietly` call is redundant and can cause double-close side effects or confusing log entries.

## Test

The existing `SnapshotEmitterTest` was updated to assert that the inner snapshot writer is not closed a second time after the outer writer is closed.
