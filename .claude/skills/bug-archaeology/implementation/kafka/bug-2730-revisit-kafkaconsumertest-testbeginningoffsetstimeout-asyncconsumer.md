**Commit**: ec9dabf86f · **Ticket**: KAFKA-17092
**Subsystem**: clients/src
**Category**: Logic error in condition

**Root cause**
Reviewers: Chia-Ping Tsai <chia7712@gmail.com>

**Before/After**
```java
// Before

        for (int i = 0; i < 10; i++) {
                    time.sleep(defaultApiTimeoutMs / 10);

// After
        if (groupProtocol == GroupProtocol.CONSUMER) {
            Node node = metadata.fetch().nodes().get(0);
            client.prepareResponseFrom(FindCoordinatorResponse.prepareResponse(Errors.NONE, groupId, node), node);
        }


        int maxPreparedResponses = GroupProtocol.CLASSIC.equals(groupProtocol) ? 10 : 1;
        for (int i = 0; i < maxPreparedResponses; i++) {
                    time.sleep(defaultApiTimeoutMs / maxPreparedResponses);
```

**How to spot**
When reviewing clients/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-17092 revisit `kafkaconsumertest#testbeginningoffsetstimeout` for asyncconsumer (#16541).

**Test**
Test modified in KafkaConsumerTest.java covering the corrected behaviour.
