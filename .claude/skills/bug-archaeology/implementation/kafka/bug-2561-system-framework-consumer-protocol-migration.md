**Commit**: 5dd4d84eec · **Ticket**: KAFKA-17272
**Subsystem**: tests/kafkatest
**Category**: Resource leak

**Root cause**
This patch adds the necessary framework for system tests of consumer protocol upgrade/downgrade paths. The change mainly includes
- adding `ConsumerProtocolConsumerEventHandler` for the consumers using the new protocol. - some other fixes to consumer_test.py with the new framework which fixes
  - [KAFKA-16576](https://issues.apache.org/jira/browse/KAFKA-16576): fixed by getting `partition_owner` after the group is fully stabilized. - [KAFKA-17219](https://issues.apache.org/jira/browse/KAFKA-17219): The first issue is the same as KAFKA-16576.

**Before/After**
```java
// Before
    def list_consumer_groups(self, node=None, command_config=None):
    def __init__(self, node, verify_offsets, idx):
        self.idx = idx
        self.state = ConsumerState.Dead
        self.revoked_count = 0
        self.assigned_count = 0
        self.assignment = []
        self.position = {}
        self.committed = {}
        self.total_consumed = 0
# This needs to be used for cooperative and consumer protocol
    def __init__(self, node, verify_offsets, idx):

// After
    def list_consumer_groups(self, node=None, command_config=None, state=None, type=None):
        if state is not None:
            cmd += " --state %s" % state
        if type is not None:
            cmd += " --type %s" % type
    def __init__(self, node, verify_offsets, idx, state=ConsumerState.Dead,
                 revoked_count=0, assigned_count=0, assignment=None,
                 position=None, committed=None, total_consumed=0):
        self.idx = idx
        self.state = state
        self.revoked_count = revoked_count
        self.assigned_count = assigned_count
```

**How to spot**
When reviewing tests/kafkatest code, look for resource leak patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-17272: [1/2] system test framework for consumer protocol migration (#16845).

**Test**
Test modified in kafka.py covering the corrected behaviour.
