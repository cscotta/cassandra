**Commit**: b348b556be · **Ticket**: KAFKA-17202
**Subsystem**: streams/src
**Category**: Wrong serialization / deserialization

**Root cause**
Reviewers: Chia-Ping Tsai <chia7712@gmail.com>

**Before/After**
```java
// Before
        final KafkaConsumer<String, String> consumer = new KafkaConsumer<>(consumerConfig(CLUSTER.bootstrapServers(), Serdes.ByteArray().deserializer().getClass(), Serdes.ByteArray().deserializer().getClass()));
        final List<TopicPartition> partitions = Collections.singletonList(tp);
        consumer.assign(partitions);
        consumer.seekToEnd(partitions);
        final long topicEndOffset = consumer.position(tp);

        assertTrue(topicEndOffset >= checkpointedOffset,
                "changelog topic end " + topicEndOffset + " is less than checkpointed offset " + checkpointedOffset);

        consumer.seekToBeginning(partitions);

        Long maxRecordOffset = null;

// After
        try (
            KafkaConsumer<String, String> consumer = new KafkaConsumer<>(
                consumerConfig(
                    CLUSTER.bootstrapServers(),
                    Serdes.ByteArray().deserializer().getClass(),
                    Serdes.ByteArray().deserializer().getClass()
                )
            )
        ) {
            final List<TopicPartition> partitions = Collections.singletonList(tp);
            consumer.assign(partitions);
            consumer.seekToEnd(partitions);
```

**How to spot**
When reviewing streams/src code, look for wrong serialization / deserialization patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-17202 surround consumer with try-resource statement (#16702).

**Test**
Test modified in EosIntegrationTest.java covering the corrected behaviour.
