**Commit**: 4ff83dc733 · **Ticket**: KAFKA-16957
**Subsystem**: clients/src
**Category**: Logic error in condition

**Root cause**
Reviewers: Kirk True <kirk@kirktrue.pro>, Chia-Ping Tsai <chia7712@gmail.com>

**Before/After**
```java
// Before
    @EnumSource(value = GroupProtocol.class, names = "CLASSIC")

// After
    @EnumSource(value = GroupProtocol.class)
        CLIENT_IDS.clear();
```

**How to spot**
When reviewing clients/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-16957 enable kafkaconsumertest#configurableobjectsshouldseegeneratedclientid to work with classic and consumer (#1.

**Test**
Test modified in KafkaConsumerTest.java covering the corrected behaviour.
