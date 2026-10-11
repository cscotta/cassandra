**Commit**: 455c79c339 · **Ticket**: KAFKA-17509
**Subsystem**: core/src
**Category**: Logic error in condition

**Root cause**
Add purgatory actions to DelayedActionQueue when partition locks are released after fetch in forceComplete. Reviewers: David Arthur <mumrah@gmail.com>, Apoorv Mittal <apoorvmittal10@gmail.com>, Jun Rao <junrao@gmail.com>

**Before/After**
```java
// Before
                             directoryEventHandler);
            Map<SharePartitionKey, SharePartition> partitionCacheMap) {

// After
import kafka.server.DelayedActionQueue;
                             directoryEventHandler,
                             new DelayedActionQueue());
import kafka.server.ActionQueue;
import kafka.server.DelayedOperationPurgatory;
    private final ActionQueue delayedActionQueue;
    private final DelayedOperationPurgatory<DelayedShareFetch> delayedShareFetchPurgatory;
            Map<SharePartitionKey, SharePartition> partitionCacheMap,
            ActionQueue delayedActionQueue,
            DelayedOperationPurgatory<DelayedShareFetch> delayedShareFetchPurgatory) {
        this.delayedActionQueue = delayedActionQueue;
        this.delayedShareFetchPurgatory = delayedShareFetchPurgatory;
```

**How to spot**
When reviewing core/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-17509: introduce a delayed action queue to complete purgatory actions outside purgatory. (#17177).

**Test**
Test modified in DelayedShareFetchTest.java covering the corrected behaviour.
