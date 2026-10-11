# Bug 1437: Fix Hint Replay with Many Accumulated Expired Hints

**Commit**: 98bcf40226 · **Ticket**: CASSANDRA-6998
**Subsystem**: src/java/org/apache/cassandra/db/HintedHandOffManager.java
**Category**: State not cleaned up

---

## Root Cause

When delivering hints to multiple endpoints, `HintedHandOffManager` would compact the hints table at the end of each individual delivery run (gated on `finished || rowsReplayed >= tombstoneWarnThreshold`). With many accumulated expired hints, the compaction was either skipped entirely (when neither condition was met for a given endpoint) or triggered redundantly N-1 times for N endpoints. More critically, the tombstones written by hint deletions were never flushed to disk before compaction ran, so the compaction could not reclaim the space they represented. The fix restructures the flow: a single major compaction runs once before all deliveries (to evict expired hints), a flush-then-compact is issued when an endpoint goes live and triggers individual replay, and a final flush (not a full compaction) is done after each delivery run to persist the tombstones.

### Before / After

```java
// BEFORE - compact() was fire-and-forget per delivery, flush not guaranteed before compact
protected Future<?> compact() {
    hintStore.forceBlockingFlush();  // flush inside compact()
    ...
    return CompactionManager.instance.submitUserDefined(...);  // async, not waited on
}
// End of doDeliverHintsToEndpoint:
if (finished || rowsReplayed.get() >= DatabaseDescriptor.getTombstoneWarnThreshold())
    compact().get();
```

```java
// AFTER - compact() is synchronous; flush before compact in deleteHintsForDeadEndpoint;
// scheduleAllDeliveries() runs one compact() upfront; doDeliverHintsToEndpoint just flushes
protected void compact() {
    // no flush here — caller is responsible
    CompactionManager.instance.submitUserDefined(...).get();  // blocking
}
// End of doDeliverHintsToEndpoint:
hintStore.forceBlockingFlush();  // flush tombstones; compaction already done upfront
```

---

## How to Spot

When a cleanup sequence involves "write tombstones, then compact to reclaim space," verify that the memtable containing those tombstones is flushed to disk before the compaction is submitted. Compaction only sees SSTables, not in-memory data. Also watch for async compaction futures that are submitted but not waited on — if the next operation depends on compaction having completed, the race will manifest under load.

---

## Test

`HintedHandOffTest` is simplified: the explicit `FBUtilities.waitOnFuture(compact())` plus busy-wait on `CompactionManager` pending/active tasks is replaced with the now-synchronous `compact()` call.
