**Commit**: 92f61b36f4 · **Ticket**: KAFKA-18226
**Subsystem**: core/src
**Category**: Logic error in condition

**Root cause**
Reviewers: Mickael Maison <mickael.maison@gmail.com>

**Before/After**
```java
// Before
      zkConnectOrNull,
import kafka.zk.ConfigEntityChangeNotificationZNode
import org.junit.jupiter.api.{AfterEach, BeforeEach, TestInfo}
    zkClient.makeSurePersistentPathExists(ConfigEntityChangeNotificationZNode.path)
    createScramCredentials(zkConnect, JaasTestUtils.KAFKA_SCRAM_ADMIN, JaasTestUtils.KAFKA_SCRAM_ADMIN_PASSWORD)
    adminZkClient.deleteTopic(largeTopic)
    TestUtils.createTopic(zkClient, topic, assignment, servers)
    val cfgs = TestUtils.createBrokerConfigs(serverCount, zkConnectOrNull, interBrokerSecurityProtocol = Some(securityProtocol),
    val cfgs = TestUtils.createBrokerConfigs(brokerCount, zkConnectOrNull, interBrokerSecurityProtocol = Some(securityProtocol),

// After
      null,
import org.junit.jupiter.api.{AfterEach, BeforeEach, Disabled, TestInfo}
@Disabled("KAFKA-18213")
    createScramCredentials("", JaasTestUtils.KAFKA_SCRAM_ADMIN, JaasTestUtils.KAFKA_SCRAM_ADMIN_PASSWORD)
    deleteTopic(largeTopic)
    TestUtils.createTopic(null, topic, assignment, servers)
    val cfgs = TestUtils.createBrokerConfigs(serverCount, null, interBrokerSecurityProtocol = Some(securityProtocol),
    val cfgs = TestUtils.createBrokerConfigs(brokerCount, null, interBrokerSecurityProtocol = Some(securityProtocol),
```

**How to spot**
When reviewing core/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-18226: disable customquotacallbacktest and remove iskrafttest (#18166).

**Test**
Test modified in BaseProducerSendTest.scala covering the corrected behaviour.
