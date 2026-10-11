# Bug 1678: Fix Concurrency Issue in StreamTransferTask

**Commit**: b1a3070477 · **Ticket**: CASSANDRA-7007
**Subsystem**: streaming/StreamTransferTask
**Category**: Race condition / missing synchronization

---

## Root Cause

`StreamTransferTask.complete()` and `scheduleTimeout()` both access the shared `files` map and the `timeoutExecutor`, but neither method was synchronized. When streaming completed files concurrently (e.g., multiple sections finishing at the same time), two threads could race inside `complete()`: one could see the map as non-empty and proceed to check whether all files were done, while another simultaneously removed the same entry, leading to incorrect completion detection or double-completion of the stream session. Similarly, `scheduleTimeout()` could race with `complete()` over the executor state. The fix adds `synchronized` to both methods so that file removal and timeout scheduling are serialised on the task instance.

### Before / After

```java
// BEFORE
public void complete(int sequenceNumber) { ... }
public ScheduledFuture scheduleTimeout(final int sequenceNumber, long time, TimeUnit unit) { ... }
```

```java
// AFTER
public synchronized void complete(int sequenceNumber) { ... }
public synchronized ScheduledFuture scheduleTimeout(final int sequenceNumber, long time, TimeUnit unit) { ... }
```

---

## How to Spot

Methods that modify or read shared mutable state (maps, counters, executor references) and may be called from multiple threads should be reviewed for missing synchronisation. A common signal is a `ConcurrentHashMap` being used for storage while the surrounding compound check-then-act logic is not atomic.

---

## Test

No test added.
