**Commit**: 140d35c545 · **Ticket**: KAFKA-8779
**Subsystem**: checkstyle/import-control-core.xml
**Category**: Logic error in condition

**Root cause**
Reviewers: Chia-Ping Tsai <chia7712@gmail.com>

**Before/After**
```java
// Before
import org.apache.kafka.common.requests.{DeleteRecordsRequest}
import org.junit.jupiter.api.{AfterEach, BeforeEach, Disabled, TestInfo, Timeout}
  private val changedBrokerLoggers = scala.collection.mutable.Set[String]()
    teardownBrokerLoggers()
        new ConfigEntry("kafka.controller.KafkaController", LogLevelConfig.INFO_LOG_LEVEL)
    val logCleanerLogLevelConfig = loggerConfig.get("kafka.cluster.Replica")
    assertEquals(kafkaLogLevel, logCleanerLogLevelConfig.value())
    assertEquals("kafka.cluster.Replica", logCleanerLogLevelConfig.name())
    assertEquals(ConfigSource.DYNAMIC_BROKER_LOGGER_CONFIG, logCleanerLogLevelConfig.source())
    assertEquals(false, logCleanerLogLevelConfig.isReadOnly)
    assertEquals(false, logCleanerLogLevelConfig.isSensitive)
    assertTrue(logCleanerLogLevelConfig.synonyms().isEmpty)

// After
    <allow pkg="org.apache.log4j" />
import org.apache.kafka.common.requests.DeleteRecordsRequest
import org.apache.log4j.PropertyConfigurator
import org.junit.jupiter.api.{AfterEach, BeforeEach, TestInfo, Timeout}
    // Due to the fact that log4j is not re-initialized across tests, changing a logger's log level persists
    // across test classes. We need to clean up the changes done after testing.
    resetLogging()
        new ConfigEntry("kafka.server.ControllerServer", LogLevelConfig.INFO_LOG_LEVEL)
    val clusterReplicaLogLevel = loggerConfig.get("kafka.cluster.Replica")
    assertEquals(kafkaLogLevel, clusterReplicaLogLevel.value())
    assertEquals("kafka.cluster.Replica", clusterReplicaLogLevel.name())
    assertEquals(ConfigSource.DYNAMIC_BROKER_LOGGER_CONFIG, clusterReplicaLogLevel.source())
```

**How to spot**
When reviewing checkstyle/import-control-core.xml code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-8779 fix flaky tests introduced by dynamic log levels (#17382).

**Test**
Test modified in PlaintextAdminIntegrationTest.scala covering the corrected behaviour.
