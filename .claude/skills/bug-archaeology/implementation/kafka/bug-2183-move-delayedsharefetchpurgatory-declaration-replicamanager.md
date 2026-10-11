**Commit**: cb3b03377d · **Ticket**: KAFKA-17742
**Subsystem**: core/src
**Category**: Logic error in condition

**Root cause**
Declare the delayed share fetch purgatory inside ReplicaManager along with the existing purgatories. Check the share fetch purgatory when a replica becomes the follower or a replica is deleted from a broker through ReplicaManager. Perform a checkAndComplete for share fetch when HWM is updated. Reviewers:  Andrew Schofield <aschofield@confluent.io>,  Apoorv Mittal <apoorvmittal10@gmail.com>, Jun Rao <junrao@gmail.com>

**Before/After**
```java
// Before
            sharePartitionManager.addPurgatoryCheckAndCompleteDelayedActionToActionQueue(
                topicPartitionData.keySet(), shareFetchData.groupId());
    DelayedShareFetchPartitionKey(Uuid topicId, int partition) {
import kafka.server.DelayedOperationPurgatory;
     * The delayed share fetch purgatory is used to store the share fetch requests that could not be processed immediately.
    private final DelayedOperationPurgatory<DelayedShareFetch> delayedShareFetchPurgatory;
        DelayedOperationPurgatory<DelayedShareFetch> delayedShar

// After
import kafka.server.share.DelayedShareFetch;
    private Optional<DelayedOperationPurgatory<DelayedShareFetch>> delayedShareFetchPurgatory = Optional.empty();
                             OptionConverters.toScala(delayedShareFetchPurgatory),
            replicaManager.addToActionQueue(() -> {
                topicPartitionData.keySet().forEach(topicIdPartition ->
                    replicaManager.completeDelayedShareFetchRequest(
                        new DelayedShareFetchGroupKey(shareFetchData.groupId(), topicIdPartition.topicId(), topicIdPartition.partition())));
                return BoxedUnit.UNIT;
            });
    public DelayedShareFetchPartitionKey(Uuid topicId, int partition) {
import kafka.server.ReplicaManager;
     * The replica manager is used to check to see if any delayed share fetch request can be completed because of data
```

**How to spot**
When reviewing core/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-17742: move delayedsharefetchpurgatory declaration to replicamanager (#17437).

**Test**
Test modified in DelayedShareFetchTest.java covering the corrected behaviour.
