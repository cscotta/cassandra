**Commit**: a8f6fc9cc4 · **Ticket**: KAFKA-18631
**Subsystem**: checkstyle/import-control.xml
**Category**: Logic error in condition

**Root cause**
Reviewers: Chia-Ping Tsai <chia7712@gmail.com>

**Before/After**
```java
// Before
        <allow class="org.apache.kafka.server.config.ZkConfigs" />
    val changedZKLoggerConfig = describeBrokerLoggers()
    assertEquals(LogLevelConfig.ERROR_LOG_LEVEL, changedZKLoggerConfig.get("kafka.log.LogCleaner").value())
    // DynamicBrokerConfig#updateBrokerConfig is used to update configs from ZooKeeper during
    // startup and when configs are updated in ZK. Update should apply valid configs and ignore
import org.apache.kafka.server.config.{DelegationTokenManagerConfigs, KRaftConfigs, QuotaConfig, ReplicationConfigs, ServerConfigs, ServerLogConfigs, ServerTopicConfigSynonyms, ZkConfigs}

        case ZkConfigs.ZK_SSL_CLIENT_ENABLE_CONFIG => assertPropertyInvalid(baseProperties, name, "not_a_boolean")
        case ZkConfigs.ZK_CLIENT_CNXN_SOCKET_CONFIG =>  //ignore string
        case ZkConfigs.ZK_SSL_KEY_STORE_LOCATION_CONFIG =>  //ignore string
        case ZkConfigs.ZK_SSL_KEY_STORE_PASSWORD_CONFIG =>  //ignore string
        case ZkConfigs.ZK_SSL_KEY_STORE_TYPE_CONFIG =>  //ignore string

// After
    val changedBrokerLoggerConfig = describeBrokerLoggers()
    assertEquals(LogLevelConfig.ERROR_LOG_LEVEL, changedBrokerLoggerConfig.get("kafka.log.LogCleaner").value())
    // DynamicBrokerConfig#updateBrokerConfig is used to update configs from broker during
    // startup and when configs are updated in broker. Update should apply valid configs and ignore
import org.apache.kafka.server.config.{DelegationTokenManagerConfigs, KRaftConfigs, QuotaConfig, ReplicationConfigs, ServerConfigs, ServerLogConfigs, ServerTopicConfigSynonyms}

```

**How to spot**
When reviewing checkstyle/import-control.xml code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-18631 remove zkconfigs (#18693).

**Test**
Test modified in PlaintextAdminIntegrationTest.scala covering the corrected behaviour.
