**Commit**: 3603c8fe35 · **Ticket**: KAFKA-18829
**Subsystem**: clients/src
**Category**: Logic error in condition

**Root cause**
Reviewers: Andrew Schofield <aschofield@confluent.io>

**Before/After**
```java
// Before
            } else if (acknowledgementMode == AcknowledgementMode.PENDING) {
                // The second call to poll(Duration) if PENDING moves into IMPLICIT

// After
            } else if (acknowledgementMode == AcknowledgementMode.PENDING && !currentFetch.isEmpty()) {
                // If there are records to acknowledge and PENDING, moves into IMPLICIT
    @ClusterTest
    public void testImplicitModeNotTriggeredByPollWhenNoAcksToSend() throws InterruptedException {
        setup();
        alterShareAutoOffsetReset("group1", "earliest");
        try (Producer<byte[], byte[]> producer = createProducer();
             ShareConsumer<byte[], byte[]> shareConsumer = createShareConsumer("group1")) {

            shareConsumer.subscribe(Collections.singleton(tp.topic()));

            Map<TopicPartition, Set<Long>> partitionOffsetsMap1 = new HashMap<>();
```

**How to spot**
When reviewing clients/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-18829: added check before converting to implicit mode (#18964).

**Test**
Test modified in ShareConsumerTest.java covering the corrected behaviour.
