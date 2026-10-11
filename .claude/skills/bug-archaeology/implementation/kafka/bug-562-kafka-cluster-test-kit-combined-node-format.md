**Commit**: 051fb66fe6 · **Ticket**: KAFKA-19938

**Subsystem**: core/src/test/java/kafka/testkit/KafkaClusterTestKit.java

**Category**: Logic error in condition

**Root cause**
`KafkaClusterTestKit` formatted storage directories during test-cluster startup. For combined broker+controller nodes the code passed `!nodes.isCombined(...)` as a flag to `formatNode`, meaning it only formatted the metadata directory for non-combined nodes; the broker data directory of a combined node was therefore never initialised. The fix restructures the loop to skip the entire broker format call for combined nodes (since the combined node's format was already handled in the controller loop) rather than passing a boolean that suppressed only part of the formatting work.

**Before**
```java
// Before: combined node only formatted metadata dir, skipped data dir
futures.add(executorService.submit(() ->
    formatNode(broker.sharedServer().metaPropsEnsemble(),
        !nodes.isCombined(broker.id()))));
```

**After**
```java
// After: combined nodes skipped entirely in broker loop
if (!nodes.isCombined(broker.id())) {
    futures.add(executorService.submit(() ->
        formatNode(broker.sharedServer().metaPropsEnsemble())));
}
```

**How to spot**
In `KafkaClusterTestKit`, grep for `formatNode` calls that receive a boolean controlling whether combined-node directories are formatted; if a combined node still appears in the broker-formatting loop, the guard condition is misplaced.

**Test**
Start a `KafkaClusterTestKit` with combined nodes and assert that all directories are properly formatted without errors during cluster startup.
