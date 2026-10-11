**Commit**: e294ea433c · **Ticket**: KAFKA-17012
**Subsystem**: clients/src
**Category**: Logic error in condition

**Root cause**
Reviewers: Chia-Ping Tsai <chia7712@gmail.com>

**Before/After**
```java
// Before
    // TODO: this test triggers a bug with the CONSUMER group protocol implementation.
    //       The bug will be investigated and fixed so this test can use both group protocols.
    @EnumSource(value = GroupProtocol.class, names = "CLASSIC")
    // TODO: this test triggers a bug with the CONSUMER group protocol implementation.
    //       The bug will be investigated and fixed so this test can use both group protocols.
    @EnumSource(value = GroupProtocol.class, names = "CLASSIC")
    // TODO: this test triggers a bug with the CONSUMER group protocol implementation.
    //       The bug will be investigated and fixed so this test can use both group protocols.
    @EnumSource(value = GroupProtocol.class, names = "CLASSIC")
    // TODO: this test requires rebalance logic which is not yet implemented in the CONSUMER group protocol.
    //       Once it is implemented, this should use both group protocols.
    @EnumSource(value = GroupProtocol.class, names = "CLASSIC")

// After
    @EnumSource(value = GroupProtocol.class)
    @EnumSource(value = GroupProtocol.class)
    @EnumSource(value = GroupProtocol.class)
    @EnumSource(value = GroupProtocol.class)
    @EnumSource(value = GroupProtocol.class)
    @EnumSource(value = GroupProtocol.class)
    @EnumSource(value = GroupProtocol.class)
```

**How to spot**
When reviewing clients/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-17012 enable consumer protocol for some tests of kafkaconsumertest (#16415).

**Test**
Test modified in KafkaConsumerTest.java covering the corrected behaviour.
