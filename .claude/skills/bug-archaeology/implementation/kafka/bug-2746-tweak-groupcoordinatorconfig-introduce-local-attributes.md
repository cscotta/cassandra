**Commit**: a533e246e3 · **Ticket**: KAFKA-17081
**Subsystem**: clients/src
**Category**: Logic error in condition

**Root cause**
Reviewers: Chia-Ping Tsai <chia7712@gmail.com>

**Before/After**
```java
// Before
    require(groupCoordinatorConfig.offsetCommitRequiredAcks >= -1 && groupCoordinatorConfig.offsetCommitRequiredAcks <= groupCoordinatorConfig.offsetsTopicReplicationFactor,
      "offsets.commit.required.acks must be greater or equal -1 and less or equal to offsets.topic.replication.factor")
    // New group coordinator configs validation.
    require(groupCoordinatorConfig.consumerGroupMaxHeartbeatIntervalMs >= groupCoordinatorConfig.consumerGroupMinHeartbeatIntervalMs,
      s"${GroupCoordinatorConfig.CONSUMER_GROUP_MAX_HEARTBEAT_INTERVAL_MS_CONFIG} must be greater than or equals " +
      s"to ${GroupCoordinatorConfig.CONSUMER_GROUP_MIN_HEARTBEAT_INTERVAL_MS_CONFIG}")
    require(groupCoordinatorConfig.consumerGroupHeartbeatIntervalMs >= groupCoordinatorConfig.consumerGroupMinHeartbeatIntervalMs,
      s"${GroupCoordinatorConfig.CONSUMER_GROUP_HEARTBEAT_INTERVAL_MS_CONFIG} must be greater than or equals " +
      s"to ${GroupCoordinatorConfig.CONSUMER_GROUP_MIN_HEARTBEAT_INTERVAL_MS_CONFIG}")
    require(groupCoordinatorConfig.consumerGroupHeartbeatIntervalMs <= groupCoordinatorConfig.consumerGroupMaxHeartbeatIntervalMs,
      s"${GroupCoordinatorConfig.CONSUMER_GROUP_HEARTBEAT_INTERVAL_MS_CONFIG} must be less than or equals " +
      s"to ${GroupCoordinatorConfig.CONSUMER_GROUP_MAX_HEARTBEAT_INTERVAL_MS_CONFIG}")

// After
    public static void require(boolean requirement, String errorMessage) {
        if (!requirement)
            throw new IllegalArgumentException(errorMessage);
    }


import static org.apache.kafka.common.utils.Utils.require;
 * This configuration utilizes several local variables instead of calling AbstractConfig#get.... as all configs here
 * are static and non-dynamic, with some being accessed extremely frequently (e.g., offsets.commit.timeout.ms).
 * Using local variable is advantageous as it avoids the overhead of repeatedly looking up these configurations in AbstractConfig.
```

**How to spot**
When reviewing clients/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-17081 tweak groupcoordinatorconfig: re-introduce local attributes and validation (#16524).

**Test**
Test modified in GroupCoordinatorConfigTest.java covering the corrected behaviour.
