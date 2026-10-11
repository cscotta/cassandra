**Commit**: 214bb07d6a · **Ticket**: KAFKA-19845
**Subsystem**: clients/src
**Category**: Logic error in condition

**Root cause**
Implements AcknowledgeType.RENEW in the share consumer as part of
KIP-1222. There will be a future PR with additional tests. Reviewers: Apoorv Mittal <apoorvmittal10@gmail.com>, Shivsundar R
 <shr@confluent.io>, Abhinav Dixit <adixit@confluent.io>

**Before/After**
```java
// Before
            createTopic("topic");

// After
    private Uuid tpId;
            tpId = createTopic("topic");
    @ClusterTest
    public void testRenewAcknowledgementOnPoll() {
        alterShareAutoOffsetReset("group1", "earliest");
        try (Producer<byte[], byte[]> producer = createProducer();
             ShareConsumer<byte[], byte[]> shareConsumer = createShareConsumer(
                 "group1",
                 Map.of(ConsumerConfig.SHARE_ACKNOWLEDGEMENT_MODE_CONFIG, EXPLICIT))
        ) {
            AtomicInteger acknowledgementsCommitted = new AtomicInteger(0);
            shareConsumer.setAcknowledgementCommitCallback((offsetsByTopicPartition, exception) ->
```

**How to spot**
When reviewing clients/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-19845: [1/n] renew acks in share consumer (#20838).

**Test**
Test modified in ShareConsumerTest.java covering the corrected behaviour.
