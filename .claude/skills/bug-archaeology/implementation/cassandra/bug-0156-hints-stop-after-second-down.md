# Bug 0156: Hints Stop Being Recorded After a Node Goes Down a Second Time

**Commit**: 5fb562d7ef · **Ticket**: CASSANDRA-19495
**Subsystem**: `hints/HintsWriteExecutor`, `hints/HintsBufferPool`, `hints/HintsDispatchTrigger`, `hints/HintsService`
**Category**: State not cleaned up

---

## Root Cause

`HintsWriteExecutor.flush()` cleared the "earliest hint" entry for a given host ID from `HintsBuffer` immediately after closing the writer for that host during a buffer-overflow flush. This was intentional so the buffer would not hold a stale minimum-timestamp for a host whose hints had just been rolled to a new file. However, the earliest-hint entry is also used by the hints-window check to decide whether new hints should be accepted for a down node. After the first down/recover cycle delivered all hints and the per-host entry was cleared, when the node went down a second time the earliest-hint timestamp was gone, causing the window check to fall outside `max_hint_window` and silently drop all subsequent hints.

The fix moves the `clearEarliestHintForHostId` call out of the per-file-overflow flush path (where it ran prematurely) and into `HintsDispatchTrigger` and `HintsService.excise`, which are invoked only when delivery is actually complete or the store is removed.

### Before / After

```java
// BEFORE — clears earliest hint from buffer on every file-size rollover during flush
private void flush(Iterator<ByteBuffer> iterator, HintsStore store, HintsBuffer buffer)
{
    // ...on size limit exceeded:
    try { store.closeWriter(); }
    finally { buffer.clearEarliestHintForHostId(store.hostId); }
}
```

```java
// AFTER — clears only when dispatch completes or store is excised
private void flush(Iterator<ByteBuffer> iterator, HintsStore store)
{
    // ...on size limit exceeded: just close, do not clear
    store.closeWriter();
}

// In HintsDispatchTrigger.run():
if (store.isWriting())
    writeExecutor.closeWriter(store);
HintsService.instance.getHintsBufferPool().clearEarliestHintsForHostId(store.hostId);

// In HintsService.excise():
catalog.exciseStore(hostId);
bufferPool.clearEarliestHintsForHostId(hostId);
```

---

## How to Spot

When a "cleanup" operation that resets tracking state is performed as a side-effect of an unrelated size/resource limit event rather than being tied to the logical lifecycle event (delivery completion / store removal), it will fire prematurely whenever the resource limit is hit. Always ask: "should this state be cleared because we finished the task, or merely because we hit a resource boundary?"

---

## Test

`HintsMaxWindowTest.testHintsKeepRecordingAfterNodeGoesOfflineRepeatedly` starts a 2-node cluster, shuts node 2 down twice with hint delivery in between, then verifies that `StorageMetrics.totalHints` grows after the second shutdown and that hints stop being recorded once `max_hint_window` is exceeded.
