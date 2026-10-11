**Commit**: 243e8e2830 · **Ticket**: KAFKA-17272
**Subsystem**: tests/kafkatest
**Category**: Logic error in condition

**Root cause**
This patch adds `consumer_protocol_migration_test.py` that tests the upgrade/downgrade paths between the old and new group protocol in KIP-848. A successful test result can be found [here](https://confluent-open-source-kafka-branch-builder-system-test-results.s3-us-west-2.amazonaws.com/dlyu-test-sem-versions/2024-10-21--001/report.html)

Reviewers: David Jacot <djacot@confluent.io>

**Before/After**
```java
// Before
                 dynamicRaftQuorum=False

// After
CONSUMER_GROUP_MIGRATION_POLICY = "group.consumer.migration.policy"
                 consumer_group_migration_policy=None,
                 dynamicRaftQuorum=False,
        :param consumer_group_migration_policy: The config that enables converting the non-empty classic group using the consumer embedded protocol to the non-empty consumer group using the consumer group protocol and vice versa.
        # Set consumer_group_migration_policy based on context and arguments.
        if consumer_group_migration_policy is None:
            arg_name = 'consumer_group_migration_policy'
            if context.injected_args is not None:
                consumer_group_migration_policy = context.injected_args.get(arg_name)
            if consumer_group_migration_policy is None:
                consumer_group_migration_policy = context.globals.get(arg_name)
        self.consumer_group_migration_policy = consumer_group_migration_policy
```

**How to spot**
When reviewing tests/kafkatest code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-17272 [2/2]: system tests for protocol migration (#17503).

**Test**
Test modified in config_property.py covering the corrected behaviour.
