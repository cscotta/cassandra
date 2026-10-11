**Commit**: 7fdd11295c · **Ticket**: KAFKA-18685
**Subsystem**: core/src
**Category**: State not cleaned up

**Root cause**
Reviewers: Chia-Ping Tsai <chia7712@gmail.com>, Christo Lolov <lolovc@amazon.com>

**Before/After**
```java
// Before
    addBrokerReconfigurable(new DynamicLogConfig(kafkaServer.logManager, kafkaServer))
class DynamicLogConfig(logManager: LogManager, server: KafkaBroker) extends BrokerReconfigurable with Logging {
    val dynamicLogConfig = new DynamicLogConfig(mock(classOf[LogManager]), mock(classOf[KafkaBroker]))
    val dynamicLogConfig = new DynamicLogConfig(mock(classOf[LogManager]), mock(classOf[KafkaBroker]))
    val dynamicLogConfig = new DynamicLogConfig(mock(classOf[LogManager]), mock(classOf[KafkaBroker]))
    config.dynamicConfig.addBrokerReconfigurable(new DynamicLogConfig(logManagerMock, serverMock))

// After
    addBrokerReconfigurable(new DynamicLogConfig(kafkaServer.logManager))
class DynamicLogConfig(logManager: LogManager) extends BrokerReconfigurable with Logging {
    val dynamicLogConfig = new DynamicLogConfig(mock(classOf[LogManager]))
    val dynamicLogConfig = new DynamicLogConfig(mock(classOf[LogManager]))
    val dynamicLogConfig = new DynamicLogConfig(mock(classOf[LogManager]))
    config.dynamicConfig.addBrokerReconfigurable(new DynamicLogConfig(logManagerMock))
```

**How to spot**
When reviewing core/src code, look for state not cleaned up patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-18685: cleanup dynamiclogconfig constructor (#18764).

**Test**
Test modified in DynamicBrokerConfigTest.scala covering the corrected behaviour.
