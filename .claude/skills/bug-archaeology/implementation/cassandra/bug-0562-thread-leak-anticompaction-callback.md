# Bug 0562: Thread Leak When Remote Nodes Fail Anticompaction

**Commit**: 7f634feb7c · **Ticket**: CASSANDRA-15002
**Subsystem**: src/java/org/apache/cassandra/db/repair, src/java/org/apache/cassandra/repair/consistent
**Category**: Resource leak

---

## Root Cause

`LocalSessions.prepareSession` registered an `onSuccess`/`onFailure` callback pair on the anticompaction future but placed `executor.shutdown()` only in the happy path of `onSuccess`, not in a `finally` block and not in `onFailure`. When a remote node failed its anticompaction, `onFailure` was called but the executor was never shut down, permanently leaking the thread. A separate bug was that `getAntiCompactionIterator` constructed `CompactionIterator` with `abortable=false`, bypassing the `AbortableUnfilteredPartitionTransformation` that allows in-progress compactions to be cancelled, so a stuck anticompaction could block session teardown indefinitely.

### Before / After

```java
// BEFORE
public void onSuccess(@Nullable Object result) {
    logger.debug("Prepare phase ... completed", sessionID);
    setStateAndSave(session, PREPARED);
    sendMessage(coordinator, new PrepareConsistentResponse(...));
    executor.shutdown();   // missing from onFailure
}
public void onFailure(Throwable t) {
    logger.error("Prepare phase ... failed", sessionID, t);
    sendMessage(coordinator, new PrepareConsistentResponse(..., false));
    failSession(sessionID, false);
    // executor.shutdown() MISSING
}
```

```java
// AFTER
public void onSuccess(@Nullable Object result) {
    try { setStateAndSave(session, PREPARED); sendMessage(...); }
    finally { executor.shutdown(); }
}
public void onFailure(Throwable t) {
    try { logger.error(...); sendMessage(...); failSession(...); }
    finally { executor.shutdown(); }
}
```

---

## How to Spot

Any `FutureCallback` (Guava) or `ListenableFuture` listener that allocates a resource (thread pool, file handle, lock) before submitting the future must shut down that resource in every callback branch, ideally using a `finally` block. A code review heuristic: search for `executor.shutdown()` inside a `FutureCallback.onSuccess` and confirm a matching call exists in `onFailure` and in a `finally` block. Asymmetric resource release across success and failure callbacks is a textbook resource-leak pattern.

---

## Test

`testRetries` and `testRetriesTimeout` verify that `AcquisitionCallable` retries acquisition on `SSTableAcquisitionException` and returns `null` after the configured timeout elapses.
