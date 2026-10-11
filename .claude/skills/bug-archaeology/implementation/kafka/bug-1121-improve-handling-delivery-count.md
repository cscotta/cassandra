**Commit**: 8dbf56e4b5 · **Ticket**: KAFKA-17541
**Subsystem**: core/src
**Category**: Resource leak

**Root cause**
For records which are automatically released as a result of closing a
share session normally, the delivery count should not be incremented. These records were fetched but they were not actually delivered to the
client since the disposition of the delivery records is carried in the
ShareAcknowledge which closes the share session. Any remaining records
were not delivered, only fetched. This PR releases the delivery count for records when closing a share
session normally.

**Before/After**
```java
// Before
            ShareConsumer<byte[], byte[]> shareConsumer = createShareConsumer("group1");
            shareConsumer.subscribe(Set.of(tp.topic()));

// After
    @ClusterTest
    public void testDeliveryCountNotIncreaseAfterSessionClose() {
        alterShareAutoOffsetReset("group1", "earliest");
        try (Producer<byte[], byte[]> producer = createProducer()) {
            ProducerRecord<byte[], byte[]> record = new ProducerRecord<>(tp.topic(), tp.partition(), null, "key".getBytes(), "value".getBytes());
            // We write 10 records to the topic, so they would be written from offsets 0-9 on the topic.
            for (int i = 0; i < 10; i++) {
                assertDoesNotThrow(() -> producer.send(record).get(), "Failed to send records");
            }
        }

        // Perform the fetch, close in a loop.
```

**How to spot**
When reviewing core/src code, look for resource leak patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-17541:[1/2] improve handling of delivery count  (#19430).

**Test**
Test modified in ShareConsumerTest.java covering the corrected behaviour.
