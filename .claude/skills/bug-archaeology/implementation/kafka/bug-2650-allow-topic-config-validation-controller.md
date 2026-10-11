**Commit**: 1b11fef5bb · **Ticket**: KAFKA-17205
**Subsystem**: core/src
**Category**: Logic error in condition

**Root cause**
Reviewers: Kamal Chandraprakash <kamal.chandraprakash@gmail.com>, Christo Lolov <lolovc@amazon.com>

**Before/After**
```java
// Before
    config: util.Map[String, String]
        config.forEach((key, value) => {
        LogConfig.validate(properties, kafkaConfig.extractLogConfigMap,
        config.forEach((key, value) => properties.setProperty(key, value))
    LogConfig.validate(config,
    LogConfig.validate(configs,
import org.junit.jupiter.api.{AfterEach, BeforeEach , TestInfo, Timeout}

// After
    newConfigs: util.Map[String, String],
    oldConfigs: util.Map[String, String]
        newConfigs.forEach((key, value) => {
        LogConfig.validate(oldConfigs, properties, kafkaConfig.extractLogConfigMap,
        newConfigs.forEach((key, value) => properties.setProperty(key, value))
    LogConfig.validate(Collections.emptyMap(), config,
    LogConfig.validate(Collections.emptyMap(), configs,
  // The remote storage config validation on controller level only works in KRaft
  @ParameterizedTest
  @ValueSource(strings = Array("kraft"))
  def testUpdateTopicConfigWithDisablingRemoteStorage(quorum: String): Unit = {
    val admin = createAdminClient()
```

**How to spot**
When reviewing core/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-17205: allow topic config validation in controller level in kraft mode (#16693).

**Test**
Test modified in RemoteTopicCrudTest.scala covering the corrected behaviour.
