**Commit**: bc24cac412 · **Ticket**: KAFKA-18371
**Subsystem**: storage/src
**Category**: Logic error in condition

**Root cause**
Reviewers: Chia-Ping Tsai <chia7712@gmail.com>

**Before/After**
```java
// Before
                ", commonProps=" + commonProps +
                ", consumerProps=" + consumerProps +
                ", producerProps=" + producerProps +

// After
import org.apache.kafka.clients.admin.AdminClientConfig;
import static org.apache.kafka.common.utils.ConfigUtils.configMapToRedactedString;
                ", commonProps=" + configMapToRedactedString(commonProps, AdminClientConfig.configDef()) +
                ", consumerProps=" + configMapToRedactedString(consumerProps, ConsumerConfig.configDef()) +
                ", producerProps=" + configMapToRedactedString(producerProps, ProducerConfig.configDef()) +
import org.apache.kafka.common.config.SaslConfigs;
import org.apache.kafka.common.config.SslConfigs;
import java.util.Arrays;
import static org.junit.jupiter.api.Assertions.assertTrue;
    @Test
    void verifyToStringRedactsSensitiveConfigurations() {
        Map<String, Object> commonClientConfig = new HashMap<>();
```

**How to spot**
When reviewing storage/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-18371 topicbasedremotelogmetadatamanagerconfig exposes sensitive configuration data in logs (#18349).

**Test**
Test modified in TopicBasedRemoteLogMetadataManagerConfigTest.java covering the corrected behaviour.
