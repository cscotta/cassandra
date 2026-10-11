**Commit**: 19e8c447a6 · **Ticket**: KAFKA-17137
**Subsystem**: core/src
**Category**: Logic error in condition

**Root cause**
Reviewers: Chia-Ping Tsai <chia7712@gmail.com>

**Before/After**
```java
// Before
import org.apache.kafka.clients.consumer.{Consumer, ConsumerConfig, KafkaConsumer, ShareConsumer}
import org.apache.kafka.common.{ConsumerGroupState, ElectionType, IsolationLevel, ShareGroupState, TopicCollection, TopicPartition, TopicPartitionInfo, TopicPartitionReplica, Uuid}
            val part = new TopicPartition(testTopicName, 0)
            parts.containsKey(part) && (parts.get(part).offset() == 1)

// After
import org.apache.kafka.clients.consumer.{Consumer, ConsumerConfig, KafkaConsumer, OffsetAndMetadata, ShareConsumer}
import org.apache.kafka.common.{ConsumerGroupState, ElectionType, GroupType, IsolationLevel, ShareGroupState, TopicCollection, TopicPartition, TopicPartitionInfo, TopicPartitionReplica, Uuid}
          TestUtils.waitUntilTrue(() => {
            val options = new ListConsumerGroupsOptions().withTypes(Set(GroupType.CLASSIC).asJava)
            val matching = client.listConsumerGroups(options).all.get.asScala.filter(group =>
                group.groupId == testGroupId &&
                group.state.get == ConsumerGroupState.STABLE)
            matching.size == 1
          }, s"Expected to be able to list $testGroupId in group type Classic")

          TestUtils.waitUntilTrue(() => {
            val options = new ListConsumerGroupsOptions().withTypes(Set(GroupType.CLASSIC).asJava)
```

**How to spot**
When reviewing core/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-17137 ensure admin apis are properly tested (consumer group and quota) (#16717).

**Test**
Test modified in PlaintextAdminIntegrationTest.scala covering the corrected behaviour.
