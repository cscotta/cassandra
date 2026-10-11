**Commit**: 9cbb3f0a4f · **Ticket**: KAFKA-17526
**Subsystem**: core/src
**Category**: Logic error in condition

**Root cause**
Reviewers: Chia-Ping Tsai <chia7712@gmail.com>

**Before/After**
```java
// Before

    private List<String> alterOpts;
        alterOpts = generateDefaultAlterOpts(cluster.bootstrapServers());
            alterAndVerifyConfig(client, Optional.of(defaultBrokerId), singletonMap("message.max.bytes", "110000"));
            alterAndVerifyConfig(client, Optional.empty(), singletonMap("message.max.bytes", "120000"));
            alterAndVerifyConfig(client, Optional.of(defaultBrokerId), singletonMap("message.max.bytes", "130000"));
            alterAndVerifyConfig(client, Optional.empty(), singletonMap("message.max.bytes", "140000"));
            deleteAndVerifyConfigValue(client, defaultBrokerId, singleton("message.max.bytes"), true);
                    singletonMap("listener.name.internal.ssl.keystore.location", "/tmp/test.jks"));
                            singletonMap("listener.name.internal.ssl.keystore.location", "/tmp/test.jks")));
                    singleton("listener.name.internal.ssl.keystore.location"), false);
                    singletonMap("listener.name.external.ssl.keystore.password", "secret"));

// After
import static org.apache.kafka.common.config.SslConfigs.SSL_TRUSTSTORE_LOCATION_CONFIG;
import static org.apache.kafka.common.config.SslConfigs.SSL_TRUSTSTORE_PASSWORD_CONFIG;
import static org.apache.kafka.common.config.SslConfigs.SSL_TRUSTSTORE_TYPE_CONFIG;
import static org.apache.kafka.coordinator.group.GroupConfig.CONSUMER_HEARTBEAT_INTERVAL_MS_CONFIG;
import static org.apache.kafka.coordinator.group.GroupConfig.CONSUMER_SESSION_TIMEOUT_MS_CONFIG;
import static org.apache.kafka.server.config.ReplicationConfigs.AUTO_LEADER_REBALANCE_ENABLE_CONFIG;
import static org.apache.kafka.server.config.ServerConfigs.MESSAGE_MAX_BYTES_CONFIG;
import static org.apache.kafka.server.config.ServerLogConfigs.AUTO_CREATE_TOPICS_ENABLE_CONFIG;
        List<String> alterOpts = generateDefaultAlterOpts(cluster.bootstrapServers());
            alterAndVerifyConfig(client, Optional.of(defaultBrokerId), singletonMap(MESSAGE_MAX_BYTES_CONFIG, "110000"), alterOpts);
            alterAndVerifyConfig(client, Optional.empty(), singletonMap(MESSAGE_MAX_BYTES_CONFIG, "120000"), alterOpts);
            alterAndVerifyConfig(client, Optional.of(defaultBrokerId), singletonMap(MESSAGE_MAX_BYTES_CONFIG, "130000"), alterOpts);
```

**How to spot**
When reviewing core/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-17526 make configcommandintegrationtest.java test use correct arguments in testing alias (#17201).

**Test**
Test modified in ConfigCommandIntegrationTest.java covering the corrected behaviour.
