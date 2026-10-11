# Bug 1331: SSTableSimpleUnsortedWriter.close() Hangs Forever When Disk Writer Thread Has Crashed

**Commit**: 6214e35401 · **Ticket**: CASSANDRA-8807
**Subsystem**: src/java/org/apache/cassandra/io/sstable/SSTableSimpleUnsortedWriter.java
**Category**: Resource leak

---

## Root Cause

`SSTableSimpleUnsortedWriter.close()` called `writeQueue.put(SENTINEL)` inside a `try` block that followed an unguarded `sync()` call. If the background disk-writer thread had already died (e.g. due to a disk-full error), the queue would never be drained and `writeQueue.put(SENTINEL)` would block indefinitely because the queue was full and no consumer was alive to empty it. The fix routes the `SENTINEL` through the same `put()` helper used for data buffers, which polls the queue with a 1-second timeout and re-checks for a writer exception on each iteration, so the caller will eventually see the exception and unblock.

### Before / After

```java
// BEFORE
sync();
try {
    writeQueue.put(SENTINEL);  // blocks forever if disk writer is dead and queue is full
    diskWriter.join();
} catch (InterruptedException e) { ... }
checkForWriterException();
```

```java
// AFTER
sync();
put(SENTINEL);  // uses timed offer() loop that checks for writer exception each iteration
try {
    diskWriter.join();
} catch (InterruptedException e) { ... }
// checkForWriterException() removed; exception surfaced through put()
```

---

## How to Spot

Any producer that writes to a bounded blocking queue and relies on a separate consumer thread to drain it must guard against the consumer dying unexpectedly. Using an unbounded `put()` or `offer()` without a timeout is safe only if the consumer is guaranteed to be alive. Reviewers should check that every queue-write path either uses a timed offer with a liveness check or can tolerate an infinite wait.

---

## Test

No test added.
