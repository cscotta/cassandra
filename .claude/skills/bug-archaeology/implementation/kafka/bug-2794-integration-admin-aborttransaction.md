**Commit**: 0353337f5f · **Ticket**: KAFKA-16650
**Subsystem**: core/src
**Category**: Logic error in condition

**Root cause**
Reviewers: Chia-Ping Tsai <chia7712@gmail.com>

**Before/After**
```java
// Before
import org.apache.kafka.clients.consumer.{Consumer, ConsumerConfig}
import org.apache.kafka.clients.producer.{KafkaProducer, ProducerRecord}
import org.apache.kafka.common.{ConsumerGroupState, ElectionType, TopicCollection, TopicPartition, TopicPartitionInfo, TopicPartitionReplica, Uuid}

// After
import org.apache.kafka.clients.consumer.{Consumer, ConsumerConfig, KafkaConsumer}
import org.apache.kafka.clients.consumer.internals.ConsumerProtocol
import org.apache.kafka.clients.producer.{KafkaProducer, ProducerConfig, ProducerRecord}
import org.apache.kafka.common.serialization.{ByteArrayDeserializer, ByteArraySerializer}
import org.apache.kafka.common.{ConsumerGroupState, ElectionType, IsolationLevel, TopicCollection, TopicPartition, TopicPartitionInfo, TopicPartitionReplica, Uuid}
  @ParameterizedTest
  @ValueSource(strings = Array("zk", "kraft", "kraft+kip848"))
  def testAbortTransaction(quorum: String): Unit = {
    client = createAdminClient
    val tp = new TopicPartition("topic1", 0)
    client.createTopics(Collections.singletonList(new NewTopic(tp.topic(), 1, 1.toShort))).all().get()

```

**How to spot**
When reviewing core/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-16650 add integration test for admin#aborttransaction (#16429).

**Test**
Test modified in PlaintextAdminIntegrationTest.scala covering the corrected behaviour.
