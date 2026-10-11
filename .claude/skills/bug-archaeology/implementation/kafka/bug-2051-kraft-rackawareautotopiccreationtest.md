**Commit**: 9e01a92155 · **Ticket**: KAFKA-15722
**Subsystem**: core/src
**Category**: Logic error in condition

**Root cause**
Reviewers: Colin P. McCabe <cmccabe@apache.org>

**Before/After**
```java
// Before

import kafka.admin.{RackAwareMode, RackAwareTest}
import kafka.utils.TestUtils
import org.junit.jupiter.api.Test
      TestUtils.createBrokerConfig(node, zkConnect, enableControlledShutdown = false, rack = Some((node / 2).toString))
  @Test
  def testAutoCreateTopic(): Unit = {
      TestUtils.waitUntilLeaderIsElectedOrChanged(zkClient, topic, 0)
      val assignment = zkClient.getReplicaAssignmentForTopics(Set(topic)).map { case (topicPartition, replicas) =>
        topicPartition.partition -> replicas
      }
      val brokerMetadatas = adminZkClient.getBrokerMetadatas(RackAwareMode.Enforced)

// After
import kafka.admin.RackAwareTest
import kafka.utils.{TestInfoUtils, TestUtils}
import org.apache.kafka.clients.admin.Admin
import org.apache.kafka.common.network.ListenerName
import org.apache.kafka.common.security.auth.SecurityProtocol
import org.junit.jupiter.api.{AfterEach, BeforeEach, TestInfo}
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource

import scala.jdk.CollectionConverters.ListHasAsScala
  var admin: Admin = _
      TestUtils.createBrokerConfig(node, null, enableControlledShutdown = false, rack = Some((node / 2).toString))
```

**How to spot**
When reviewing core/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-15722: add kraft support in rackawareautotopiccreationtest.

**Test**
Test modified in RackAwareAutoTopicCreationTest.scala covering the corrected behaviour.
