**Commit**: 2f9c2dd828 · **Ticket**: KAFKA-16718
**Subsystem**: clients/src
**Category**: Wrong serialization / deserialization

**Root cause**
This is a follow up PR for implementation of DeleteShareGroupOffsets
RPC. This PR adds the ShareGroupStatePartitionMetadata record to
__consumer__offsets topic to make sure the topic is removed from the
initializedTopics list. This PR also removes partitions from the request
and response schemas for DeleteShareGroupState RPC

Reviewers: Sushant Mahajan <smahajan@confluent.io>, Andrew Schofield <aschofield@confluent.io>

**Before/After**
```java
// Before
     * Delete offsets for a set of partitions in a share group.
     * @param partitions The topic-partitions.
    DeleteShareGroupOffsetsResult deleteShareGroupOffsets(String groupId, Set<TopicPartition> partitions, DeleteShareGroupOffsetsOptions options);
     * Delete offsets for a set of partitions in a share group with the default options.
     * @param partitions The topic-partitions.
    default DeleteShareGroupOffsetsResult deleteShareGroupOffsets(String groupId, Set<TopicPartition> partitions) {
        return deleteShareGroupOffsets(groupId, partitions, new DeleteShareGroupOffsetsOptions());
import org.apache.kafka.common.TopicPartition;
    private final KafkaFuture<Map<TopicPartition, ApiException>> future;
    private final Set<TopicPartition> partitions;
    DeleteShareGroupOffsetsResult(KafkaFuture<Map<TopicPartition, ApiException>> future, Set<TopicPartition> partitions) {
        this.partitions = partitions;

// After
     * Delete offsets for a set of topics in a share group.
     * @param topics The topics for which to delete offsets.
    DeleteShareGroupOffsetsResult deleteShareGroupOffsets(String groupId, Set<String> topics, DeleteShareGroupOffsetsOptions options);
     * Delete offsets for a set of topics in a share group with the default options.
     * @param topics The topics for which to delete offsets.
    default DeleteShareGroupOffsetsResult deleteShareGroupOffsets(String groupId, Set<String> topics) {
        return deleteShareGroupOffsets(groupId, topics, new DeleteShareGroupOffsetsOptions());
    private final KafkaFuture<Map<String, ApiException>> future;
    private final Set<String> topics;
    DeleteShareGroupOffsetsResult(KafkaFuture<Map<String, ApiException>> future, Set<String> topics) {
        this.topics = topics;
     * If not, the first topic error shall be returned.
```

**How to spot**
When reviewing clients/src code, look for wrong serialization / deserialization patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-16718-3/n: added the sharegroupstatepartitionmetadata record during deletion of share group offsets (#19478).

**Test**
Test modified in KafkaAdminClientTest.java covering the corrected behaviour.
