**Commit**: a81f08d368 · **Ticket**: KAFKA-19550
**Subsystem**: core/src
**Category**: Logic error in condition

**Root cause**
This change adds:

- Integration test for `Admin#describeStreamsGroups` API
- Integration test for `Admin#deleteStreamsGroup` API

Reviewers: Alieh Saeedi <asaeedi@confluent.io>, Lucas Brutschy
 <lucasbru@apache.org>

---------

Co-authored-by: Lucas Brutschy <lbrutschy@gmail.com>

**Before/After**
```java
// Before
import java.util.{Optional, Properties}
import org.apache.kafka.clients.consumer.internals.{AsyncKafkaConsumer, StreamsRebalanceData}

// After
import java.util
import java.util.{Optional, Properties, UUID}
import org.apache.kafka.clients.consumer.internals.{AsyncKafkaConsumer, StreamsRebalanceData, StreamsRebalanceListener}
import scala.jdk.CollectionConverters._
  def createStreamsGroup[K, V](configOverrides: Properties = new Properties,
                               configsToRemove: List[String] = List(),
                               inputTopic: String,
                               streamsGroupId: String): AsyncKafkaConsumer[K, V] = {
    val props = new Properties()
    props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers())
    props.put(ConsumerConfig.GROUP_ID_CONFIG, streamsGroupId)
    props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "false")
```

**How to spot**
When reviewing core/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-19550: integration test for streams-related admin apis [1/n] (#20244).

**Test**
Test modified in IntegrationTestHarness.scala covering the corrected behaviour.
