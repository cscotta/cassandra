**Commit**: dd784e7d7a · **Ticket**: KAFKA-16717
**Subsystem**: clients/src
**Category**: Logic error in condition

**Root cause**
[KAFKA-16717](https://issues.apache.org/jira/browse/KAFKA-16717) aims to
finish the AlterShareGroupOffsets for ShareGroupCommand part. Reviewers: Lan Ding <isDing_L@163.com>, Chia-Ping Tsai
 <chia7712@gmail.com>, TaiJuWu <tjwu1217@gmail.com>, Andrew Schofield
 <aschofield@confluent.io>

**Before/After**
```java
// Before
        public Builder addPartition(String topic, int partition, Map<String, Uuid> topicIdsToNames,  Errors error) {
                .setErrorCode(error.code())
            topic.partitions.forEach(partition => responseBuilder.addPartition(topic.topicName, partition.partitionIndex, metadataCache.topicNamesToIds, error.error))
                        //TODO build new AlterShareGroupOffsetsResponseData for error response
                        return response;

// After
        public Builder addPartition(String topic, int partition, Map<String, Uuid> topicIdsToNames, ApiError error) {
                .setErrorCode(error.error().code())
import java.util.Optional;
    public static ListOffsetsResult createListOffsetsResult(Map<TopicPartition, OffsetAndMetadata> partitionOffsets) {
        Map<TopicPartition, KafkaFuture<ListOffsetsResult.ListOffsetsResultInfo>> futures =
            partitionOffsets.entrySet().stream()
                .collect(Collectors.toMap(
                    Map.Entry::getKey,
                    entry -> KafkaFuture.completedFuture(
                        new ListOffsetsResult.ListOffsetsResultInfo(
                            entry.getValue().offset(),
                            System.currentTimeMillis(),
```

**How to spot**
When reviewing clients/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-16717 [3/n]: add adminclient.altersharegroupoffsets (#19820).

**Test**
Test modified in AdminClientTestUtils.java covering the corrected behaviour.
