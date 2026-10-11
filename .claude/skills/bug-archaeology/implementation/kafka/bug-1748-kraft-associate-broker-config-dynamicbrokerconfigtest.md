**Commit**: 1156d5c294 · **Ticket**: KAFKA-18315
**Subsystem**: core/src
**Category**: Resource leak

**Root cause**
Reviewers: PoAn Yang <payang@apache.org>, Chia-Ping Tsai <chia7712@gmail.com>

**Before/After**
```java
// Before
            "",
import kafka.zk.KafkaZkClient
    val origProps = TestUtils.createBrokerConfig(0, TestUtils.MockZkConnect, port = 8181)
    val origProps = TestUtils.createBrokerConfig(0, TestUtils.MockZkConnect, port = 8181)
  def testPasswordConfigEncryption(): Unit = {
    val props = TestUtils.createBrokerConfig(0, TestUtils.MockZkConnect, port = 8181)
    dynamicProps.put(SaslConfigs.SASL_JAAS_CONFIG, "myLoginModule required;")
    assertFalse(persistedProps.getProperty(SaslConfigs.SASL_JAAS_CONFIG).contains("myLoginModule"),
      "Password not encoded")
    val decodedProps = configWithSecret.dynamicConfig.fromPersistentProps(persistedProps, perBrokerConfig = true)
    assertEquals("myLoginModule required;", decodedProps.getProperty(SaslConfigs.SASL_JAAS_CONFIG))
    val props = TestUtils.createBrokerConfig(0, TestUtils.MockZkConnect, port = 8181)

// After
import org.apache.kafka.network.SocketServerConfigs;
            null,
        properties.put(SocketServerConfigs.LISTENER_SECURITY_PROTOCOL_MAP_CONFIG, "PLAINTEXT:PLAINTEXT,SSL:SSL");
    val origProps = TestUtils.createBrokerConfig(0, null, port = 8181)
    val origProps = TestUtils.createBrokerConfig(0, null, port = 8181)
  def testPasswordConfigNotEncryption(): Unit = {
    val props = TestUtils.createBrokerConfig(0, null, port = 8181)
    val password = "myLoginModule required;"
    dynamicProps.put(SaslConfigs.SASL_JAAS_CONFIG, password)
    assertEquals(password, persistedProps.getProperty(SaslConfigs.SASL_JAAS_CONFIG))
    val props = TestUtils.createBrokerConfig(0, null, port = 8181)
    val password = "dynamicLoginModul
```

**How to spot**
When reviewing core/src code, look for resource leak patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-18315 fix to kraft or remove tests associate with zk broker config in dynamicbrokerconfigtest, replicamanagertest,.

**Test**
Test modified in DescribeTopicPartitionsRequestHandlerTest.java covering the corrected behaviour.
