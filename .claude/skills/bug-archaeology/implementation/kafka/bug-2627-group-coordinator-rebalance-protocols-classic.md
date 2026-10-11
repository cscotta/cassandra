**Commit**: da14b5a61d · **Ticket**: KAFKA-16390
**Subsystem**: tests/kafkatest
**Category**: Logic error in condition

**Root cause**
Fix an issue that cause system test failing when using AsyncKafkaConsumer. A configuration option, group.coordinator.rebalance.protocols, was introduced to specify the rebalance protocols used by the group coordinator. By default, the rebalance protocol is set to classic. When the new group coordinator is enabled, the rebalance protocols are set to classic,consumer.

**Before/After**
```java
// Before
                config_property.NEW_GROUP_COORDINATOR_ENABLE: use_new_coordinator


// After
GROUP_COORDINATOR_REBALANCE_PROTOCOLS="group.coordinator.rebalance.protocols"
        # When the new group coordinator is enabled, the new consumer rebalance
        # protocol is enabled too.
        rebalance_protocols = "classic"
        if self.use_new_coordinator:
            rebalance_protocols = "classic,consumer"

                config_property.NEW_GROUP_COORDINATOR_ENABLE: use_new_coordinator,
                config_property.GROUP_COORDINATOR_REBALANCE_PROTOCOLS: rebalance_protocols
            override_configs[config_property.GROUP_COORDINATOR_REBALANCE_PROTOCOLS] = 'classic,consumer'

```

**How to spot**
When reviewing tests/kafkatest code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-16390: add `group.coordinator.rebalance.protocols=classic,consumer` to broker configs when system tests need the n.

**Test**
Test modified in config_property.py covering the corrected behaviour.
