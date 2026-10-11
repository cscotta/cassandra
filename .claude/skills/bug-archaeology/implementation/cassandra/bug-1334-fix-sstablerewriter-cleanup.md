# Bug 1334: SSTableRewriter Cleanup Polls Queue Before Processing Entry, Skipping Abort on Exception

**Commit**: 26c2049783 · **Ticket**: CASSANDRA-8802
**Subsystem**: src/java/org/apache/cassandra/io/sstable/SSTableRewriter.java
**Category**: State not cleaned up

---

## Root Cause

The cleanup loop in `SSTableRewriter` used `finishedEarly.poll()` at the top of each iteration to both retrieve and remove the head element. If processing that element threw an exception, the element had already been removed from the queue, so the exception handler could not retry or clean it up — the partially-processed writer was silently dropped without being aborted. The fix changes the loop to use `peek()` to inspect without removing, does all the required work (abort/close the writer, release the reader), and only calls `poll()` at the very end to remove the element after successful processing.

### Before / After

```java
// BEFORE
Finished f = finishedEarly.poll();  // removes immediately; lost on exception
if (f.writer.getFilePointer() > 0) {
    // ... process f ...
}
```

```java
// AFTER
Finished f = finishedEarly.peek();  // inspect without removing
if (f.writer.getFilePointer() > 0) {
    // ... process f ...
}
finishedEarly.poll();  // remove only after processing is complete
```

---

## How to Spot

In cleanup or rollback loops, prefer `peek()` + process + `poll()` over `poll()` + process. Removing an item before its cleanup is finished means any exception leaves the item unprocessed with no way to retry. This is a common pattern error in queue-based cleanup code: the removal and the action it guards should not be conflated into a single step at the start of the loop.

---

## Test

No test added.
