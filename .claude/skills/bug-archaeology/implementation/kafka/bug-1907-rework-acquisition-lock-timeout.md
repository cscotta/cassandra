**Commit**: 423a6353a8 · **Ticket**: KAFKA-18025
**Subsystem**: core/src
**Category**: Incorrect filtering / wrong query result

**Root cause**
Additional work on ShareConsumerTest.testAcquisitionLockTimeoutOnConsumer. First, mark the test as flaky since it fails occasionally and it would be best to get a decent number of passes before assuming it's no longer flaky. Then, change the assertions so that the test checks which records were received before it counts them (we might get too many records because the wrong records are being returned, or because records are actually being duplicated). The rare failures appear to be related to returning too many records, so it would be good to see whether we can learn more about the duplication.

**Before/After**
```java
// Before
             KafkaShareConsumer<byte[], byte[]> shareConsumer1 = createShareConsumer(new ByteArrayDeserializer(), new ByteArrayDeserializer(), "group1")) {
            shareConsumer1.subscribe(Collections.singleton(tp.topic()));
            // Poll two times to receive records. The first poll puts the acquisition lock and fetches the record.
            // Since, we are only sending one record and acquisition lock hasn't timed out, the second poll only acknowledges the
            // record from the first poll and no more fetch.
            ConsumerRecords<byte[], byte[]> records1 = shareConsumer1.poll(Duration.ofMillis(5000));
            assertEquals(1, records1.count());
            assertEquals("key_1", new String(records1.iterator().next().key()));
            assertEquals("value_1", new String(records1.iterator().next().value()));
            ConsumerRecords<byte[], byte[]> records2 = shareConsumer1.poll(Duration.ofMillis(500));
            assertEquals(0, records2.count());
            producer.send(producerRecord2);

// After
            .setConfigProp("share.coordinator.state.topic.num.partitions", "3")
    @Flaky("KAFKA-18025")
             KafkaShareConsumer<byte[], byte[]> shareConsumer = createShareConsumer(new ByteArrayDeserializer(), new ByteArrayDeserializer(), "group1")) {
            shareConsumer.subscribe(Set.of(tp.topic()));
            // Produce a first record which is consumed and acknowledged normally.
            // Poll twice to receive records. The first poll fetches the record and starts the acquisition lock timer.
            // Since, we are only sending one record and the acquisition lock hasn't timed out, the second poll only
            // acknowledges the record from the first poll and does not fetch any more records.
            ConsumerRecords<byte[], byte[]> consumerRecords = shareConsumer.poll(Duration.ofMillis(5000));
            ConsumerRecord<byte[], byte[]> consumerRecord = consumerRecords.records(tp).get(0);
            assertEquals("key_1", new String(consumerRecord.key()));
            assertEquals("value_1", new String(consumerRecord.value()));
```

**How to spot**
When reviewing core/src code, look for incorrect filtering / wrong query result patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-18025: rework acquisition lock timeout test (#17985).

**Test**
Test modified in ShareConsumerTest.java covering the corrected behaviour.
