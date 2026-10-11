**Commit**: 64b63f472f · **Ticket**: KAFKA-19316
**Subsystem**: tests/kafkatest
**Category**: Logic error in condition

**Root cause**
This PR include system tests in the file share_group_command_test.py. These tests test the functionality of kafka-share-groups.sh tool

Reviewers: Sushant Mahajan <smahajan@confluent.io>, Andrew Schofield
 <aschofield@confluent.io>

**Before/After**
```java
// Before
    def __init__(self, context, num_nodes, kafka, topic, group_id="test-share-consumer-group",
            if not (line.startswith("SLF4J") or line.startswith("TOPIC") or line.startswith("Could not fetch offset")):
group.id={{ group_id|default('test-share-consumer-group') }}

// After
    def __init__(self, context, num_nodes, kafka, topic, group_id="test-share-group",

    def list_share_groups(self, node=None, command_config=None, state=None):
        """ Get list of share groups.
        """
        if node is None:
            node = self.nodes[0]
        share_group_script = self.path.script("kafka-share-groups.sh", node)

        if command_config is None:
            command_config = ""
        else:
```

**How to spot**
When reviewing tests/kafkatest code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-19316: added share_group_command_test.py system tests (#19774).

**Test**
Test modified in console_share_consumer.py covering the corrected behaviour.
