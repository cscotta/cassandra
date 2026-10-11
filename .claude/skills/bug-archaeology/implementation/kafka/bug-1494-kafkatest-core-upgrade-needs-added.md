**Commit**: 0ff4dafb7d · **Ticket**: KAFKA-18146
**Subsystem**: tests/kafkatest
**Category**: Logic error in condition

**Root cause**
This patch renames kraft_upgrade_test.py to upgrade_test.py. This is enough to cover the old upgrade/downgrade tests. Reviewers: Chia-Ping Tsai <chia7712@gmail.com>

**Before/After**
```java
// Before
class TestKRaftUpgrade(ProduceConsumeValidateTest):
        super(TestKRaftUpgrade, self).__init__(test_context=test_context)

// After
class TestUpgrade(ProduceConsumeValidateTest):
        super(TestUpgrade, self).__init__(test_context=test_context)
```

**How to spot**
When reviewing tests/kafkatest code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-18146; tests/kafkatest/tests/core/upgrade_test.py needs to be re-added as kraft (#18766).

**Test**
Test modified in upgrade_test.py covering the corrected behaviour.
