**Commit**: a670e2b207 · **Ticket**: KAFKA-20159
**Subsystem**: core/src
**Category**: Logic error in condition

**Root cause**
Make `share.delivery.count.limit` become dynamic config, we can change
the value at run time and set to specific group. Reviewers: Andrew Schofield <aschofield@confluent.io>

**Before/After**
```java
// Before
// (no removals)

// After
    @ClusterTest
    public void testDynamicDeliveryCountLimitDecreaseArchivesRecords() {
        alterShareAutoOffsetReset("group1", "earliest");
        try (Producer<byte[], byte[]> producer = createProducer();
            ShareConsumer<byte[], byte[]> shareConsumer = createShareConsumer(
                "group1",
                Map.of(ConsumerConfig.SHARE_ACKNOWLEDGEMENT_MODE_CONFIG, EXPLICIT))
        ) {
            // Produce 1 record.
            ProducerRecord<byte[], byte[]> record = new ProducerRecord<>(tp.topic(), tp.partition(),
                null, "key".getBytes(), "value".getBytes());
            producer.send(record);
```

**How to spot**
When reviewing core/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-20159: group configuration for share.delivery.count.limit (#21491).

**Test**
Test modified in ShareConsumerTest.java covering the corrected behaviour.
