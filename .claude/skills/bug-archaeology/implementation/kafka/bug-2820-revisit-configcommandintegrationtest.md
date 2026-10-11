**Commit**: f79e429044 · **Ticket**: KAFKA-16939
**Subsystem**: core/src
**Category**: Logic error in condition

**Root cause**
Reviewers: Chia-Ping Tsai <chia7712@gmail.com>

**Before/After**
```java
// Before
            deleteAndVerifyConfig(client, Optional.of(defaultBrokerId), singleton("message.max.bytes"));
            alterConfigWithKraft(client, Optional.empty(),
                    singletonMap("listener.name.internal.ssl.keystore.location", "/tmp/test.jks"));
            deleteAndVerifyConfig(client, Optional.of(defaultBrokerId),
                    singleton("listener.name.internal.ssl.keystore.location"));
            verifyConfigDefaultValue(client, Optional.of(defaultBrokerId),
                "--entity-type", "brokers",
                "--entity-name", "0", "--alter");
        ConfigResource configResource = new ConfigResource(ConfigResource.Type.BROKER, brokerId.orElse(defaultBrokerId));
            Map<String, String> current = client.describeConfigs(singletonList(configResource))
                    .all()
                    .get()

// After
import org.apache.kafka.clients.admin.ConfigEntry;
import java.util.Objects;
            deleteAndVerifyConfigValue(client, defaultBrokerId, singleton("message.max.bytes"), true);
            // Per-broker config configured at default cluster-level should fail
            assertThrows(ExecutionException.class,
                    () -> alterConfigWithKraft(client, Optional.empty(),
                            singletonMap("listener.name.internal.ssl.keystore.location", "/tmp/test.jks")));
            deleteAndVerifyConfigValue(client, defaultBrokerId,
                    singleton("listener.name.internal.ssl.keystore.location"), false);
            verifyConfigSecretValue(client, Optional.of(defaultBrokerId),
                "--entity-type", "brokers", "--alter");
        ConfigResource configResource = new ConfigResource(ConfigResource.Type.BROKER, brokerId.orElse(""));
```

**How to spot**
When reviewing core/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-16939 revisit configcommandintegrationtest (#16317).

**Test**
Test modified in ConfigCommandIntegrationTest.java covering the corrected behaviour.
