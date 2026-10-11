**Commit**: 24689dc6ab · **Ticket**: KAFKA-17879
**Subsystem**: tests/kafkatest
**Category**: Logic error in condition

**Root cause**
Reviewers: Chia-Ping Tsai <chia7712@gmail.com>

**Before/After**
```java
// Before
            self.zk, topics={self.topic: {'partitions': 1, 'replication-factor': 1}}, version=version)

// After
            self.zk, topics={self.topic: {'partitions': 1, 'replication-factor': 1}}, version=DEV_BRANCH)
```

**How to spot**
When reviewing tests/kafkatest code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-17879 test_performance_services.py should use dev version to run kafka service (#17606).

**Test**
Test modified in test_performance_services.py covering the corrected behaviour.
