**Commit**: ff785ac251 · **Ticket**: KAFKA-18651
**Subsystem**: core/src
**Category**: Logic error in condition

**Root cause**
This change implements the broker-side configs proposed in KIP-1071. The configurations implemented by this PR are only those that were specifically aimed to be included in `AK 4.1`. Reviewers: Lucas Brutschy <lbrutschy@confluent.io>

**Before/After**
```java
// Before
      if (processRoles.isEmpty || !isNewGroupCoordinatorEnabled) {
        throw new ConfigException(s"The new '${GroupType.STREAMS}' rebalance protocol is only supported in KRaft cluster with the new group coordinator.")
      warn(s"The new '${GroupType.STREAMS}' rebalance protocol is enabled along with the new group coordinator. " +
        "This is part of the preview of KIP-1071 and MUST NOT be used in production.")
import org.apache.kafka.coordinator.group.GroupConfig.{CONSUMER_HEARTBEAT_INTERVAL_MS_CONFIG, CONSUMER_SESSION_TIMEOUT_MS_CONFIG, SHARE_AUTO_OFFSET_RESET_CONFIG, SHARE_HEARTBEAT_INTERVAL_MS_CONFIG, SHARE_RECORD_LOCK_DURATION_MS_CONFIG, SHARE_SESSION_TIMEOUT_MS_CONFIG}

// After
      if (!isNewGroupCoordinatorEnabled) {
        warn(s"The new '${GroupType.STREAMS}' rebalance protocol is only supported with the new group coordinator.")
      warn(s"Streams groups and the new '${GroupType.STREAMS}' rebalance protocol are enabled. " +
        "This is part of the early access of KIP-1071 and MUST NOT be used in production.")
import org.apache.kafka.coordinator.group.GroupConfig.{CONSUMER_HEARTBEAT_INTERVAL_MS_CONFIG, CONSUMER_SESSION_TIMEOUT_MS_CONFIG, SHARE_AUTO_OFFSET_RESET_CONFIG, SHARE_HEARTBEAT_INTERVAL_MS_CONFIG, SHARE_RECORD_LOCK_DURATION_MS_CONFIG, SHARE_SESSION_TIMEOUT_MS_CONFIG, STREAMS_HEARTBEAT_INTERVAL_MS_CONFIG, STREAMS_NUM_STANDBY_REPLICAS_CONFIG, STREAMS_SESSION_TIMEOUT_MS_CONFIG}
    cgConfigs.put(STREAMS_HEARTBEAT_INTERVAL_MS_CONFIG, GroupCoordinatorConfig.STREAMS_GROUP_HEARTBEAT_INTERVAL_MS_DEFAULT.toString)
    cgConfigs.put(STREAMS_SESSION_TIMEOUT_MS_CONFIG, GroupCoordinatorConfig.STREAMS_GROUP_SESSION_TIMEOUT_MS_DEFAULT.toString)
    cgConfigs.put(STREAMS_NUM_STANDBY_REPLICAS_CONFIG, GroupCoordinatorConfig.STREAMS_GROUP_NUM_STANDBY_REPLICAS_DEFAULT.toString)

        /** Streams groups configs */
        case GroupCoordinatorConfig.STREAMS_GROUP_SESSION_TIMEOUT_MS_CONFIG => assertPropertyInvalid(baseProperties, name, "not_a_number", 0, -1)
        case GroupCoordinatorConfig.STREAMS_GROUP_MIN_SESSION_TIMEOUT_MS_CONFIG => assertPropertyInvalid(baseProperties, name, "not_a_number", 0, -1)
```

**How to spot**
When reviewing core/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-18651: add streams-specific broker configurations (#19176).

**Test**
Test modified in KafkaApisTest.scala covering the corrected behaviour.
