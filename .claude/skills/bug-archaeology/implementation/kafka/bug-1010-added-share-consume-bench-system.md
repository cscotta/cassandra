**Commit**: d1f41ef011 · **Ticket**: KAFKA-19320
**Subsystem**: tests/kafkatest
**Category**: Logic error in condition

**Root cause**
This PR adds system tests in share_consume_bench_test.py for testing the
trogdor agent for Share Consumers/

Reviewers: Lan Ding <53332773+DL1231@users.noreply.github.com>, Andrew
 Schofield <aschofield@confluent.io>

**Before/After**
```java
// Before
              (share_group_script,
               self.bootstrap_servers(self.security_protocol),
               command_config)

// After
               (share_group_script,
                self.bootstrap_servers(self.security_protocol),
                command_config)
    def set_share_group_offset_reset_strategy(self, group, strategy=None, node=None):
        """ Set the offset reset strategy config for the given group.
        """
        if strategy is None:
            return
        if node is None:
            node = self.nodes[0]
        consumer_group_script = self.path.script("kafka-configs.sh", node)

```

**How to spot**
When reviewing tests/kafkatest code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-19320: added share_consume_bench_test.py system tests (#19811).

**Test**
Test modified in kafka.py covering the corrected behaviour.
