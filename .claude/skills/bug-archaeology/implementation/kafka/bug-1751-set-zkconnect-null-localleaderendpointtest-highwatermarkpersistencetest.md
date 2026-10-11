**Commit**: 91a2b58616 · **Ticket**: KAFKA-18359
**Subsystem**: core/src
**Category**: State not cleaned up

**Root cause**
Reviewers: TaiJuWu <tjwu1217@gmail.com>, Chia-Ping Tsai <chia7712@gmail.com>

**Before/After**
```java
// Before
    val props = TestUtils.createBrokerConfig(sourceBroker.id, TestUtils.MockZkConnect, port = sourceBroker.port)
  val configs = TestUtils.createBrokerConfigs(2, TestUtils.MockZkConnect).map(KafkaConfig.fromProps)
  val configs = TestUtils.createBrokerConfigs(2, TestUtils.MockZkConnect).map(KafkaConfig.fromProps(_, overridingProps))
  val configs = TestUtils.createBrokerConfigs(2, TestUtils.MockZkConnect).map(KafkaConfig.fromProps(_, new Properties()))
  private val config = TestUtils.createBrokerConfigs(1, TestUtils.MockZkConnect).map(KafkaConfig.fromProps).head

// After
    val props = TestUtils.createBrokerConfig(sourceBroker.id, null, port = sourceBroker.port)
  val configs = TestUtils.createBrokerConfigs(2, null).map(KafkaConfig.fromProps)
  val configs = TestUtils.createBrokerConfigs(2, null).map(KafkaConfig.fromProps(_, overridingProps))
  val configs = TestUtils.createBrokerConfigs(2, null).map(KafkaConfig.fromProps(_, new Properties()))
  private val config = TestUtils.createBrokerConfigs(1, null).map(KafkaConfig.fromProps).head
```

**How to spot**
When reviewing core/src code, look for state not cleaned up patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-18359 set zkconnect to null in localleaderendpointtest, highwatermarkpersistencetest, isrexpirationtest, replicama.

**Test**
Test modified in LocalLeaderEndPointTest.scala covering the corrected behaviour.
