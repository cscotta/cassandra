**Commit**: fc21a1553d · **Ticket**: KAFKA-19027
**Subsystem**: tools/src
**Category**: Logic error in condition

**Root cause**
jira: https://issues.apache.org/jira/browse/KAFKA-19027

[KAFKA-18329](https://issues.apache.org/jira/browse/KAFKA-18329) will
remove old coordinator, so `ConsumerGroupCommandTestUtils#generator`
creates only one config now. Hence, we should use ClusterTestDefaults to
write more readable code for tests. Reviewers: Ken Huang <s7133700@gmail.com>, PoAn Yang
<payang@apache.org>, Chia-Ping Tsai <chia7712@gmail.com>

**Before/After**
```java
// Before
import org.apache.kafka.common.test.api.ClusterConfig;
import org.apache.kafka.server.common.Feature;
import java.util.HashMap;
import java.util.Map;
import static org.apache.kafka.common.test.api.Type.CO_KRAFT;
import static org.apache.kafka.coordinator.group.GroupCoordinatorConfig.CONSUMER_GROUP_HEARTBEAT_INTERVAL_MS_CONFIG;
import static org.apache.kafka.coordinator.group.GroupCoordinatorConfig.CONSUMER_GROUP_MIN_HEARTBEAT_INTERVAL_MS_CONFIG;
import static org.apache.kafka.coordinator.group.GroupCoordinatorConfig.GROUP_INITIAL_REBALANCE_DELAY_MS_CONFIG;
import static org.apache.kafka.coordinator.group.GroupCoordinatorConfig.OFFSETS_TOPIC_PARTITIONS_CONFIG;
import static org.apache.kafka.coordinator.group.GroupCoordinatorConfig.OFFSETS_TOPIC_REPLICATION_FACTOR_CONFIG;
 * The old test framework {@link kafka.api.BaseConsumerTest#getTestGroupProtocolParametersAll} test for the following cases:
 * <ul>

// After
 * This class provides methods to build and manage consumer instances.
import org.ap
```

**How to spot**
When reviewing tools/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-19027 replace consumergroupcommandtestutils#generator by clustertestdefaults (#19347).

**Test**
Test modified in ConsumerGroupCommandTestUtils.java covering the corrected behaviour.
