# Bug 0596: Unbounded Validation Compactions During Repair

**Commit**: 00e5a3d508 · **Ticket**: CASSANDRA-14332
**Subsystem**: `org.apache.cassandra.repair`
**Category**: Race condition / missing synchronization

---

## Root Cause

A previous commit (CASSANDRA-13797) removed the `Futures.getUnchecked(validations)` call from `RepairJob.run()`, intending to make validation compactions run asynchronously. The unintended consequence was that the repair coordinator no longer waited for validation to complete before moving on to the synchronization phase. This allowed an unbounded number of concurrent validation compactions to accumulate — each repair round could queue new validations before prior rounds finished — exhausting thread-pool and disk resources. Reverting to the blocking wait restores back-pressure so each repair job's validation phase must complete before the next phase proceeds.

### Before / After

```java
// BEFORE – validation just queued, coordinator moves on immediately
Futures.addCallback(validations, new FutureCallback<List<TreeResponse>>() { ... }, taskExecutor);
// (no wait)
```

```java
// AFTER – coordinator blocks until all validators respond
Futures.addCallback(validations, new FutureCallback<List<TreeResponse>>() { ... }, taskExecutor);

// Wait for validation to complete
Futures.getUnchecked(validations);
```

---

## How to Spot

Fire-and-forget patterns on `ListenableFuture` collections inside a staged pipeline are risky: if downstream stages can also enqueue work, missing `get()` or equivalent barrier calls enable unbounded queue growth. Any repair, compaction, or streaming pipeline where a "wait" call was removed as a "performance improvement" should be scrutinized to confirm the back-pressure it provided is replaced by another mechanism.

---

## Test

No test added.
