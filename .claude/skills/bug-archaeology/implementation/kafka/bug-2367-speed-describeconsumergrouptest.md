**Commit**: 95b734d3c8 · **Ticket**: KAFKA-17472
**Subsystem**: tools/src
**Category**: Logic error in condition

**Root cause**
Reviewers: Chia-Ping Tsai <chia7712@gmail.com>

**Before/After**
```java
// Before
// (no removals)

// After
import static org.apache.kafka.coordinator.group.GroupCoordinatorConfig.CONSUMER_GROUP_HEARTBEAT_INTERVAL_MS_CONFIG;
import static org.apache.kafka.coordinator.group.GroupCoordinatorConfig.CONSUMER_GROUP_MIN_HEARTBEAT_INTERVAL_MS_CONFIG;
import static org.apache.kafka.coordinator.group.GroupCoordinatorConfig.GROUP_INITIAL_REBALANCE_DELAY_MS_CONFIG;
        serverProperties.put(GROUP_INITIAL_REBALANCE_DELAY_MS_CONFIG, "1000");
        serverProperties.put(CONSUMER_GROUP_HEARTBEAT_INTERVAL_MS_CONFIG, "500");
        serverProperties.put(CONSUMER_GROUP_MIN_HEARTBEAT_INTERVAL_MS_CONFIG, "500");
        serverProperties.put(GROUP_INITIAL_REBALANCE_DELAY_MS_CONFIG, "1000");
        serverProperties.put(CONSUMER_GROUP_HEARTBEAT_INTERVAL_MS_CONFIG, "500");
        serverProperties.put(CONSUMER_GROUP_MIN_HEARTBEAT_INTERVAL_MS_CONFIG, "500");
```

**How to spot**
When reviewing tools/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-17472 speed up describeconsumergrouptest (#17117).

**Test**
Test modified in ConsumerGroupCommandTestUtils.java covering the corrected behaviour.
