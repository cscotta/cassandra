**Commit**: 29254d6816 · **Ticket**: KAFKA-20425
**Subsystem**: core/src
**Category**: Logic error in condition

**Root cause**
The delayed share fetch triggers waiting requests for same topic
partition in purgatory for which the request has been completed. This is
needed as for same sahre partition there can be requests in purgatory
waiting to acquire lock. However, the triggers are always not needed
i.e. when no data is available in partition hence these triggers can be
avoided.

**Before/After**
```java
// Before
            List<ShareFetchPartitionData> shareFetchPartitionDataList = new ArrayList<>();
            releasePartitionLocksAndAddToActionQueue(topicPartitionData.keySet());
        releasePartitionLocksAndAddToActionQueue(nonRemoteFetchTopicPartitions);
            releasePartitionLocksAndAddToActionQueue(partitionsAcquired.keySet());
    private void releasePartitionLocksAndAddToActionQueue(Set<TopicIdPartition> topicIdPartitions) {
        if (topicIdPartitions.isEmpty()) {
        releasePartitionLocks(topicIdPartitions);
        replicaManager.addToActionQueue(() -> topicIdPartitions.forEach(topicIdPartition -> {

// After
import java.util.HashSet;
        List<ShareFetchPartitionData> shareFetchPartitionDataList = new ArrayList<>();
            releasePartitionLocksAndAddToActionQueue(topicPartitionData.keySet(), partitionsWithData(shareFetchPartitionDataList));
        releasePartitionLocksAndAddToActionQueue(nonRemoteFetchTopicPartitions, nonRemoteFetchTopicPartitions);
            releasePartitionLocksAndAddToActionQueue(partitionsAcquired.keySet(), partitionsAcquired.keySet());
    private Set<TopicIdPartition> partitionsWithData(List<ShareFetchPartitionData> shareFetchPartitionDataList) {
        if (shareFetchPartitionDataList == null || shareFetchPartitionDataList.isEmpty()) {
            return Set.of();
        }
        Set<TopicIdPartition> partitionsWithData = new HashSet<>();
        shareFetchPartitionDataList.forEach(shareFetchPartitionData -> {
            if (shareFetchPartitionData.fetchPartitionData() != null &&
```

**How to spot**
When reviewing core/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-20425: optimize purgatory triggers when no data is available for share fetch (#22005).

**Test**
Test modified in DelayedShareFetchTest.java covering the corrected behaviour.
