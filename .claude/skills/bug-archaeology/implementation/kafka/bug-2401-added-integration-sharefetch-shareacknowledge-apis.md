**Commit**: e380b7f5d1 · **Ticket**: KAFKA-17290
**Subsystem**: core/src
**Category**: Logic error in condition

**Root cause**
This PR contains Integration Tests for Share Fetch and Share Acknowledge APIs

Reviewers:  Apoorv Mittal <apoorvmittal10@gmail.com>,  Manikumar Reddy <manikumar.reddy@gmail.com>

**Before/After**
```java
// Before
    val partitions = new util.LinkedHashMap[TopicIdPartition, ShareFetchResponseData.PartitionData](responsePartitionData.asJava)
import org.apache.kafka.common.TopicPartition
import java.util.Comparator

// After
    val partitions = new util.LinkedHashMap[TopicIdPartition, ShareFetchResponseData.PartitionData](responsePartitionData.asJava)
import kafka.network.SocketServer
import org.apache.kafka.clients.producer.{KafkaProducer, ProducerRecord, RecordMetadata}
import org.apache.kafka.common.{TopicIdPartition, TopicPartition, Uuid}
import org.apache.kafka.common.serialization.StringSerializer
import org.apache.kafka.controller.ControllerRequestContextUtil.ANONYMOUS_CONTEXT
import java.util.{Comparator, Properties}
import scala.collection.Seq
import scala.collection.convert.ImplicitConversions.{`collection AsScalaIterable`, `map AsScala`}
  protected var producer: KafkaProducer[String, String] = _

  protected def createTopicAndReturnLeaders(
```

**How to spot**
When reviewing core/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-17290: added integration tests for sharefetch and shareacknowledge apis (#16916).

**Test**
Test modified in GroupCoordinatorBaseRequestTest.scala covering the corrected behaviour.
