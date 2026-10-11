**Commit**: 190dd79457 · **Ticket**: KAFKA-16860
**Subsystem**: tests/kafkatest
**Category**: Resource leak

**Root cause**
This patch updates the system tests to correctly enable the new consumer protocol/coordinator in the tests requiring them. I went with the simplest approach for now. Long term, I think that we should refactor the tests to better handle features and non-production features. I got a successful run of the consumer system tests with this patch combined with https://github.com/apache/kafka/pull/16120: https://confluent-kafka-branch-builder-system-test-results.s3-us-west-2.amazonaws.com/system-test-kafka-branch-builder--1717155071--dajac--KAFKA-16860-2--29028ae0dd/2024-05-31--001./2024-05-31--001./report.html.

**Before/After**
```java
// Before
                config_property.NEW_GROUP_COORDINATOR_ENABLE: use_new_coordinator

// After
GROUP_COORDINATOR_REBALANCE_PROTOCOLS="group.coordinator.rebalance.protocols"

UNSTABLE_FEATURE_VERSIONS_ENABLE="unstable.feature.versions.enable"
        # When the new group coordinator is enabled, the new consumer rebalance
        # protocol is enabled too.
        rebalance_protocols = "classic"
        if self.use_new_coordinator:
            rebalance_protocols = "classic,consumer"

                config_property.UNSTABLE_FEATURE_VERSIONS_ENABLE: use_new_coordinator,
                config_property.NEW_GROUP_COORDINATOR_ENABLE: use_new_coordinator,
                config_property.GROUP_COORDINATOR_REBALANCE_PROTOCOLS: rebalance_protocols
```

**How to spot**
When reviewing tests/kafkatest code, look for resource leak patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-16860; [2/2] introduce group.version feature flag (#16149).

**Test**
Test modified in config_property.py covering the corrected behaviour.
