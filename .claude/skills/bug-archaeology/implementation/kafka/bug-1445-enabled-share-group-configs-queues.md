**Commit**: 0989a6a389 · **Ticket**: KAFKA-18756
**Subsystem**: tests/kafkatest
**Category**: Logic error in condition

**Root cause**
Reviewers: Andrew Schofield <aschofield@confluent.io>

**Before/After**
```java
// Before
                 use_transactions_v2=False
                                      listener_security_config=self.listener_security_config)

// After
UNSTABLE_API_VERSIONS_ENABLE = "unstable.api.versions.enable"
SHARE_COORDINATOR_STATE_TOPIC_REPLICATION_FACTOR ="share.coordinator.state.topic.replication.factor"
SHARE_COORDINATOR_STATE_TOPIC_MIN_ISR = "share.coordinator.state.topic.min.isr"
SHARE_GROUP_ENABLE = "group.share.enable"

                 use_transactions_v2=False,
                 use_share_groups=None
        :param use_share_groups: When true, enables the use of share groups introduced in KIP-932

        # Set use_share_groups based on context and arguments.
        # If not specified, the default config is used.
        if use_share_groups is None:
```

**How to spot**
When reviewing tests/kafkatest code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-18756: enabled share group configs for queues related system tests (#18829).

**Test**
Test modified in config_property.py covering the corrected behaviour.
