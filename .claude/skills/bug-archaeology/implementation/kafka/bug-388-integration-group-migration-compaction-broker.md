**Commit**: b9eae3388c · **Ticket**: KAFKA-20254
**Subsystem**: build.gradle
**Category**: Resource leak

**Root cause**
This adds integration tests to GroupCoordinatorIntegrationTest that
verify the group coordinator loads correctly after compaction and broker
restart when a group has been upgraded from the classic protocol to the
consumer or streams protocol. For each protocol upgrade path (classic-to-consumer and
classic-to-streams), there are two test variants. The first variant
compacts __consumer_offsets but retains the GroupMetadata tombstone,
verifying that replaying tombstones after compaction works correctly. The second variant forces tombstone removal by running two compaction
passes with delete.retention.ms=0 and min.cleanable.dirty.ratio=0.

**Before/After**
```java
// Before
import org.apache.kafka.clients.admin.{Admin, ConsumerGroupDescription}
import org.apache.kafka.clients.consumer.{Consumer, GroupProtocol, OffsetAndMetadata}
import org.apache.kafka.common.{ConsumerGroupState, GroupType, KafkaFuture, TopicCollection, TopicPartition}

// After
    testImplementation project(':streams')
import org.apache.kafka.clients.admin.{Admin, AlterConfigOp, ConfigEntry, ConsumerGroupDescription}
import org.apache.kafka.clients.consumer.{Consumer, ConsumerConfig, GroupProtocol, OffsetAndMetadata}
import org.apache.kafka.common.config.{ConfigResource, TopicConfig}
import org.apache.kafka.common.{ConsumerGroupState, GroupState, GroupType, KafkaFuture, TopicCollection, TopicPartition}
import org.apache.kafka.common.serialization.Serdes
import org.apache.kafka.streams.{KafkaStreams, StreamsBuilder, StreamsConfig}
import org.apache.kafka.streams.{GroupProtocol => StreamsGroupProtocol}
import java.util.Properties
  @ClusterTest(
    types = Array(Type.KRAFT),
    serverProperties = Array(
```

**How to spot**
When reviewing build.gradle code, look for resource leak patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-20254: add integration tests for group migration with compaction and broker restart (#21642).

**Test**
Test modified in GroupCoordinatorIntegrationTest.scala covering the corrected behaviour.
