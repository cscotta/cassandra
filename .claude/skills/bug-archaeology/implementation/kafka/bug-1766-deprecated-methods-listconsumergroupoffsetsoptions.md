**Commit**: 1cf514313e · **Ticket**: KAFKA-18291
**Subsystem**: clients/src
**Category**: Resource leak

**Root cause**
Reviewers: Chia-Ping Tsai <chia7712@gmail.com>

**Before/After**
```java
// Before
        @SuppressWarnings("deprecation")
        ListConsumerGroupOffsetsSpec groupSpec = new ListConsumerGroupOffsetsSpec()
            .topicPartitions(options.topicPartitions());
import org.apache.kafka.common.TopicPartition;
import java.util.List;
    private List<TopicPartition> topicPartitions;
    /**
     * Set the topic partitions to list as part of the result.
     * {@code null} includes all topic partitions.
     * <p>
     * @deprecated Since 3.3.
     * Use {@link Admin#listConsumerGroupOffsets(java.util.Map, ListConsumerGroupOffsetsOptions)}

// After
        ListConsumerGroupOffsetsSpec groupSpec = new ListConsumerGroupOffsetsSpec();
        verifyListConsumerGroupOffsetsOptions();
    private void verifyListConsumerGroupOffsetsOptions() throws Exception {

            final ListConsumerGroupOffsetsSpec groupSpec = new ListConsumerGroupOffsetsSpec()
                    .topicPartitions(partitions);
            env.adminClient().listConsumerGroupOffsets(Collections.singletonMap(GROUP_ID, groupSpec), options);
```

**How to spot**
When reviewing clients/src code, look for resource leak patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-18291 remove deprecated methods of listconsumergroupoffsetsoptions (#18265).

**Test**
Test modified in KafkaAdminClientTest.java covering the corrected behaviour.
