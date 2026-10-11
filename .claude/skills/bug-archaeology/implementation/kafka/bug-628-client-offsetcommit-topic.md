**Commit**: da596f6e2e · **Ticket**: KAFKA-15283
**Subsystem**: clients/src
**Category**: Logic error in condition

**Root cause**
Introduce Topic ID in existing OffsetCommit API, the patch only contains
the client side changes. Reviewers: David Jacot <djacot@confluent.io>, Lianet Magrans
 <lmagrans@confluent.io>, Chia-Ping Tsai <chia7712@gmail.com>

**Before/After**
```java
// Before
            OffsetCommitRequest.Builder builder = OffsetCommitRequest.Builder.forTopicNames(data);
                    TopicPartition tp = new TopicPartition(topic.name(), partition.partitionIndex());
                        long offset = offsetAndMetadata.offset();
                        log.debug("OffsetCommit completed successfully for offset {} partition {}", offset, tp);

// After
import org.apache.kafka.common.Uuid;
            Map<String, Uuid> topicIds = metadata.topicIds();
            boolean canUseTopicIds = true;
                Uuid topicId = topicIds.getOrDefault(topicPartition.topic(), Uuid.ZERO_UUID);
                if (topicId.equals(Uuid.ZERO_UUID)) {
                    canUseTopicIds = false;
                }
                            .setTopicId(topicId)
            OffsetCommitRequest.Builder builder = canUseTopicIds
                    ? OffsetCommitRequest.Builder.forTopicIdsOrNames(data, true)
                    : OffsetCommitRequest.Builder.forTopicNames(data);
        @SuppressWarnings("NPathComplexity")
```

**How to spot**
When reviewing clients/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-15283:[1/n] client support for offsetcommit with topic id (#19577).

**Test**
Test modified in CommitRequestManagerTest.java covering the corrected behaviour.
