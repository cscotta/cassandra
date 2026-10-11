# Bug 1332: StreamTransferTask Abort Stops Releasing SSTable References After First Exception

**Commit**: a762e5add2 · **Ticket**: CASSANDRA-8815
**Subsystem**: src/java/org/apache/cassandra/streaming/StreamTransferTask.java
**Category**: Resource leak

---

## Root Cause

`StreamTransferTask.abort()` iterated over all pending `OutgoingFileMessage` entries and called `file.sstable.releaseReference()` on each. If any single `releaseReference()` threw an exception the loop was aborted immediately by the uncaught exception, leaving all subsequent SSTables with their reference counts permanently elevated — a resource leak that prevented those SSTables from ever being deleted. The fix wraps each release in its own try/catch, accumulates all suppressed exceptions, clears the files map unconditionally, and then re-throws the first exception after every release has been attempted.

### Before / After

```java
// BEFORE
for (OutgoingFileMessage file : files.values())
    file.sstable.releaseReference();  // exception here abandons remaining releases
```

```java
// AFTER
Throwable fail = null;
for (OutgoingFileMessage file : files.values()) {
    try {
        file.sstable.releaseReference();
    } catch (Throwable t) {
        if (fail == null) fail = t;
        else fail.addSuppressed(t);
    }
}
files.clear();
if (fail != null)
    Throwables.propagate(fail);
```

---

## How to Spot

Cleanup loops that release multiple resources must not short-circuit on the first failure; every resource must get a release attempt regardless of what the earlier ones do. A simple review heuristic: any `for` loop over a collection of reference-counted or closeable objects where the loop body is not wrapped in try/catch is a candidate for this defect. The pattern of "collect first exception, suppress the rest, rethrow at the end" is the standard idiom to fix it.

---

## Test

No test added.
