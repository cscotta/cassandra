**Commit**: c5a78b0186 · **Ticket**: KAFKA-19358
**Subsystem**: tests/kafkatest
**Category**: Logic error in condition

**Root cause**
According to the current code in AK, the offset reset strategy for share
groups was set using the flag `--offset-reset-strategy` in the
share_consumer_test.py tests, but that would mean that the admin client
call would be sent out by all members in the share group. This PR
changes that by introducing `set_group_offset_reset_strategy` method in
kafka.py, which runs the kafka-configs.sh script in one of the existing
docker containers, thereby changing the config only once. Reviewers: Andrew Schofield <aschofield@confluent.io>

**Before/After**
```java
// Before
    def set_share_group_offset_reset_strategy(self, group, strategy=None, node=None):
        """ Set the offset reset strategy config for the given group.
        """
        if strategy is None:
            return
        if node is None:
            node = self.nodes[0]
        consumer_group_script = self.path.script("kafka-configs.sh", node)

        cmd = fix_opts_for_new_jvm(node)
        cmd += "%s --bootstrap-server %s --group %s --alter --add-config \"share.auto.offset.reset=%s\"" % \
              (consumer_group_script,

// After
    def __init__(self, context, num_nodes, kafka, topic, group_id, max_messages=-1,
                 acknowledgement_mode="auto", version=DEV_BRANCH, stop_timeout_sec=60,
                 log_level="INFO", jaas_override_variables=None, on_record_consumed=None):
```

**How to spot**
When reviewing tests/kafkatest code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-19358: updated share_consumer_test.py tests to use set_group_offset_reset_strategy (#19878).

**Test**
Test modified in kafka.py covering the corrected behaviour.
