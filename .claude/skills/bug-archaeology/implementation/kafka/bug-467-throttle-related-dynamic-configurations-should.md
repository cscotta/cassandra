**Commit**: c1c88968ad · **Ticket**: KAFKA-19984
**Subsystem**: server-common/src
**Category**: Logic error in condition

**Root cause**
The following configs are weird because those values are definitely not
READONLY
- leader.replication.throttled.rate
- follower.replication.throttled.rate
- replica.alter.log.dirs.io.max.bytes.per.second

Add them into the `DynamicBrokerConfig` to ensure when we query it's not
READONLY

Reviewers: Lan Ding <isDing_L@163.com>, Chia-Ping Tsai
 <chia7712@gmail.com>

**Before/After**
```java
// Before
            ShareCoordinatorConfig.RECONFIGURABLE_CONFIGS)

// After
    public static final Set<String> BROKER_QUOTA_CONFIGS = Set.copyOf(brokerQuotaConfigs().names());

            ShareCoordinatorConfig.RECONFIGURABLE_CONFIGS,
            QuotaConfig.BROKER_QUOTA_CONFIGS)
import org.apache.kafka.server.config.QuotaConfig;
import java.util.stream.Collectors;
import static org.junit.jupiter.api.Assertions.assertFalse;

    @ClusterTest
    public void testQuotaConfigsIsReadOnlyShouldBeFalse(ClusterInstance clusterInstance) throws Exception {
        try (Admin admin = Admin.create(adminConfig(clusterInstance, true))) {
            int nodeId = clusterInstance.controllers().values().iterator().next().config().nodeId();
```

**How to spot**
When reviewing server-common/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-19984 throttle-related dynamic configurations should not have the `isreadonly` flag (#21131).

**Test**
Test modified in BootstrapControllersIntegrationTest.java covering the corrected behaviour.
