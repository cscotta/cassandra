**Commit**: d4d9f11816 · **Ticket**: KAFKA-18761
**Subsystem**: clients/src
**Category**: Logic error in condition

**Root cause**
This PR approaches completion of Admin.listShareGroupOffsets() and
kafka-share-groups.sh --describe --offsets. Prior to this patch, kafka-share-groups.sh was only able to describe the
offsets for partitions which were assigned to active members. Now, the
Admin.listShareGroupOffsets() uses the persister's knowledge of the
share-partitions which have initialised state. Then, it uses this list
to obtain a complete set of offset information.

**Before/After**
```java
// Before
import java.util.List;
        return topicPartitions == null ? List.of() : topicPartitions;
import java.util.LinkedList;
            Map<String, List<Integer>> topicPartitionMap = new HashMap<>();
            spec.topicPartitions().forEach(tp -> topicPartitionMap.computeIfAbsent(tp.topic(), t -> new LinkedList<>()).add(tp.partition()));

            Map<String, DescribeShareGroupOffsetsRequestTopic> requestTopics = new HashMap<>();
            for (TopicPartition tp : spec.topicPartitions()) {
                requestTopics.computeIfAbsent(tp.topic(), t ->
                        new DescribeShareGroupOffsetsRequestTopic()
                            .setTopicName(tp.topic())
                            .setPartitions(new LinkedList<>()))

// After
     * {@code null} indicates that offsets of all partitions of the group are to be listed.
        return topicPartitions;
            if (spec.topicPartitions() != null) {
                Map<String, List<Integer>> topicPartitionMap = new HashMap<>();
                spec.topicPartitions().forEach(tp -> topicPartitionMap.computeIfAbsent(tp.topic(), t -> new ArrayList<>()).add(tp.partition()));

                Map<String, DescribeShareGroupOffsetsRequestTopic> requestTopics = new HashMap<>();
                for (TopicPartition tp : spec.topicPartitions()) {
                    requestTopics.computeIfAbsent(tp.topic(), t ->
                            new DescribeShareGroupOffsetsRequestTopic()
                                .setTopicName(tp.topic())
                                .setPartitions(new ArrayList<>()))
```

**How to spot**
When reviewing clients/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-18761: [2/n] list share group offsets with state and auth (#19328).

**Test**
Test modified in KafkaAdminClientTest.java covering the corrected behaviour.
