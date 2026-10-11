**Commit**: c5a31cd6fb · **Ticket**: KAFKA-17625
**Subsystem**: tests/kafkatest
**Category**: Logic error in condition

**Root cause**
This PR removes ZK test parameterizations from ducktape by:

- Removing zk from quorum.all_non_upgrade
- Removing quorum.zk from @matrix and @parametrize annotations
- Changing usages of quorum.all to quorum.all_kraft
- Deleting message_format_change_test.py

The default metadata_quorum value still needs to be changed to KRaft rather than ZK, but this will be done in a follow-up PR. Reviewers: Kirk True <kirk@kirktrue.pro>, Colin P. McCabe <cmccabe@apache.org>

**Before/After**
```java
// Before
#   [“ZK”] before the KIP-500 bridge release(s)
#   [“ZK”, “ISOLATED_KRAFT”] during the KIP-500 bridge release(s) and in preview releases
all_non_upgrade = [zk, isolated_kraft]
    @matrix(
        metadata_quorum=[quorum.zk],
        use_new_coordinator=[False]
    )
        metadata_quorum=[quorum.zk, quorum.isolated_kraft],
        metadata_quorum=[quorum.zk, quorum.isolated_kraft],
        metadata_quorum=[quorum.zk, quorum.isolated_kraft],
        metadata_quorum=[quorum.zk, quorum.isolated_kraft],
        metadata_quorum=[quorum.zk, quorum.isolated_kraft],

// After
all_non_upgrade = [isolated_kraft]
        metadata_quorum=[quorum.isolated_kraft],
        metadata_quorum=[quorum.isolated_kraft],
        metadata_quorum=[quorum.isolated_kraft],
        metadata_quorum=[quorum.isolated_kraft],
        metadata_quorum=[quorum.isolated_kraft],
        metadata_quorum=[quorum.isolated_kraft],
        metadata_quorum=[quorum.isolated_kraft],
        metadata_quorum=[quorum.isolated_kraft],
```

**How to spot**
When reviewing tests/kafkatest code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-17625: removing explicit zk test parameterizations (#17638).

**Test**
Test modified in quorum.py covering the corrected behaviour.
