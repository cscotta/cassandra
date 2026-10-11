**Commit**: 75075002b5 · **Ticket**: KAFKA-17106
**Subsystem**: clients/src
**Category**: Logic error in condition

**Root cause**
Reviewers: PoAn Yang <payang@apache.org>, Chia-Ping Tsai <chia7712@gmail.com>

**Before/After**
```java
// Before
    // TODO: this test triggers a bug with the CONSUMER group protocol implementation.
    //       The bug will be investigated and fixed so this test can use both group protocols.
    @EnumSource(value = GroupProtocol.class, names = "CLASSIC")
        Node node = metadata.fetch().nodes().get(0);

// After
    @EnumSource(value = GroupProtocol.class)

        if (groupProtocol == GroupProtocol.CONSUMER) {
            Node node = metadata.fetch().nodes().get(0);
            client.prepareResponseFrom(FindCoordinatorResponse.prepareResponse(Errors.NONE, groupId, node), node);
        }
```

**How to spot**
When reviewing clients/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-17106 enable testfetchprogresswithmissingpartitionposition for asyncconsumer (#16564).

**Test**
Test modified in KafkaConsumerTest.java covering the corrected behaviour.
