**Commit**: 241b6e6c6f · **Ticket**: KAFKA-17410
**Subsystem**: clients/src
**Category**: Logic error in condition

**Root cause**
Reviewers: Lianet Magrans <lmagrans@confluent.io>

**Before/After**
```java
// Before
    @EnumSource(GroupProtocol.class)

// After
    @EnumSource(value = GroupProtocol.class, names = "CLASSIC")
```

**How to spot**
When reviewing clients/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-17410: disable testpollthrowsinterruptexceptionifinterrupted for asyncconsumer (#17172).

**Test**
Test modified in KafkaConsumerTest.java covering the corrected behaviour.
