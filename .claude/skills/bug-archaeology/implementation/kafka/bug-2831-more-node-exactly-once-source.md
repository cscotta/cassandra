**Commit**: 191b6476d7 · **Ticket**: KAFKA-16988
**Subsystem**: tests/kafkatest
**Category**: Logic error in condition

**Root cause**
Reviewers: Igor Soarez <soarez@apple.com>

**Before/After**
```java
// Before
    @cluster(num_nodes=6)

// After
    @cluster(num_nodes=7)
```

**How to spot**
When reviewing tests/kafkatest code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-16988: add 1 more node for test_exactly_once_source system test (#16379).

**Test**
Test modified in connect_distributed_test.py covering the corrected behaviour.
