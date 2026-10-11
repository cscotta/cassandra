**Commit**: e104974b74 · **Ticket**: KAFKA-17110
**Subsystem**: clients/src
**Category**: Logic error in condition

**Root cause**
Reviewers: Chia-Ping Tsai <chia7712@gmail.com>

**Before/After**
```java
// Before
    @EnumSource(value = GroupProtocol.class, names = "CLASSIC")
    // TODO: this test triggers a bug with the CONSUMER group protocol implementation.
    //       The bug will be investigated and fixed so this test can use both group protocols.
    @EnumSource(value = GroupProtocol.class, names = "CLASSIC")
    @EnumSource(value = GroupProtocol.class, names = "CLASSIC")
    @EnumSource(value = GroupProtocol.class, names = "CLASSIC")
    // TODO: this test triggers a bug with the CONSUMER group protocol implementation.
    //       The bug will be investigated and fixed so this test can use both group protocols.
    @EnumSource(value = GroupProtocol.class, names = "CLASSIC")
    // TODO: this test triggers a bug with the CONSUMER group protocol implementation.
    //       The bug will be investigated and fixed so this test can use both group protocols.
    @EnumSource(value = GroupProtocol.class, names = "CLASSIC")

// After
import org.junit.jupiter.api.Timeout;
@Timeout(value = 120)
    @EnumSource(GroupProtocol.class)
    @EnumSource(GroupProtocol.class)
    @EnumSource(GroupProtocol.class)
    @EnumSource(GroupProtocol.class)
    @EnumSource(GroupProtocol.class)
    @EnumSource(GroupProtocol.class)
    @EnumSource(GroupProtocol.class)
    @EnumSource(GroupProtocol.class)
```

**How to spot**
When reviewing clients/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-17110 enable valid test case in kafkaconsumertest for asynckafkaconsumer (#16566).

**Test**
Test modified in KafkaConsumerTest.java covering the corrected behaviour.
