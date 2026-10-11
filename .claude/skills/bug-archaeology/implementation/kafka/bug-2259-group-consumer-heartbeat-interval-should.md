**Commit**: 18a584c90e · **Ticket**: KAFKA-17618
**Subsystem**: group-coordinator/src
**Category**: Logic error in condition

**Root cause**
This patch ensures that the heartbeat interval is smaller than the session timeout. Reviewers: David Jacot <djacot@confluent.io>

**Before/After**
```java
// Before
      new ClusterConfigProperty(key = GroupCoordinatorConfig.CONSUMER_GROUP_SESSION_TIMEOUT_MS_CONFIG, value = "5000"),
      new ClusterConfigProperty(key = GroupCoordinatorConfig.CONSUMER_GROUP_MIN_SESSION_TIMEOUT_MS_CONFIG, value = "5000")
      new ClusterConfigProperty(key = "group.share.session.timeout.ms", value = "500"),
      new ClusterConfigProperty(key = "group.share.min.session.timeout.ms", value = "500")

// After
      new ClusterConfigProperty(key = GroupCoordinatorConfig.CONSUMER_GROUP_SESSION_TIMEOUT_MS_CONFIG, value = "5001"),
      new ClusterConfigProperty(key = GroupCoordinatorConfig.CONSUMER_GROUP_MIN_SESSION_TIMEOUT_MS_CONFIG, value = "5001")
      new ClusterConfigProperty(key = "group.share.session.timeout.ms", value = "501"),
      new ClusterConfigProperty(key = "group.share.min.session.timeout.ms", value = "501")

        if (consumerSessionTimeout <= consumerHeartbeatInterval) {
            throw new InvalidConfigurationException(CONSUMER_SESSION_TIMEOUT_MS_CONFIG + " must be greater than " +
                CONSUMER_HEARTBEAT_INTERVAL_MS_CONFIG);
        }

        require(consumerGroupHeartbeatIntervalMs < consumerGroupSessionTimeoutMs,
                String.format("%s must be less than %s", CONSUMER_GROUP_HEARTBEAT_INTERVAL_MS_CONFIG, CONSUMER_GROUP_SESSION_TIMEOUT_MS_CONFIG));
```

**How to spot**
When reviewing group-coordinator/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-17618; group consumer heartbeat interval should be less than session timeout (#17281).

**Test**
Test modified in ConsumerGroupHeartbeatRequestTest.scala covering the corrected behaviour.
