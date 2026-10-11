**Commit**: 2a0dbd8e0b · **Ticket**: KAFKA-18909
**Subsystem**: core/src
**Category**: Logic error in condition

**Root cause**
* Add `DynamicThreadPool.java` to the server module. * Remove the old DynamicThreadPool object in the `DynamicBrokerConfig.scala`. Reviewers: TengYao Chi <kitingiao@gmail.com>, Chia-Ping Tsai <chia7712@gmail.com>

**Before/After**
```java
// Before
import org.apache.kafka.server.ProcessRole
import org.apache.kafka.server.config.{ReplicationConfigs, ServerConfigs, ServerLogConfigs, ServerTopicConfigSynonyms}
    DynamicThreadPool.ReconfigurableConfigs ++
object DynamicThreadPool {
  val ReconfigurableConfigs = Set(
    ServerConfigs.NUM_IO_THREADS_CONFIG,
    ReplicationConfigs.NUM_REPLICA_FETCHERS_CONFIG,
    ServerLogConfigs.NUM_RECOVERY_THREADS_PER_DATA_DIR_CONFIG,
    ServerConfigs.BACKGROUND_THREADS_CONFIG)

  def validateReconfiguration(currentConfig: KafkaConfig, newConfig: KafkaConfig): Unit = {
    newConfig.values.forEach { (k, v) =>

// After
import org.apache.kafka.server.{ProcessRole, DynamicThreadPool}
import org.apache.kafka.server.config.{ServerConfigs, ServerLogConfigs, ServerTopicConfigSynonyms}
    DynamicThreadPool.RECONFIGURABLE_CONFIGS.asScala ++
    DynamicThreadPool.RECONFIGURABLE_CONFIGS.asScala
```

**How to spot**
When reviewing core/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-18909 move dynamicthreadpool to server module (#19081).

**Test**
Test modified in DynamicBrokerConfigTest.scala covering the corrected behaviour.
