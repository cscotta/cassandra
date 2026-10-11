**Commit**: abbb6b3c13 · **Ticket**: KAFKA-19471
**Subsystem**: clients/src
**Category**: Wrong serialization / deserialization

**Root cause**
This patch mainly includes two improvements:

1. Update currentFetch when `pollForFetches()` throws an exception. 2. Add an override `KafkaShareConsumer.acknowledge(String topic, int
partition, long offset, AcknowledgeType type)` .

**Before/After**
```java
// Before
// (no removals)

// After
import org.apache.kafka.common.errors.RecordDeserializationException;
import org.apache.kafka.common.errors.SerializationException;
import java.nio.ByteBuffer;
    @ClusterTest
    public void testExplicitOverrideAcknowledgeCorruptedMessage() {
        alterShareAutoOffsetReset("group1", "earliest");
        try (Producer<byte[], byte[]> producer = createProducer();
            ShareConsumer<byte[], byte[]> shareConsumer = createShareConsumer(
                "group1",
                Map.of(ConsumerConfig.SHARE_ACKNOWLEDGEMENT_MODE_CONFIG, EXPLICIT),
                null,
                mockErrorDeserializer(3))) {
```

**How to spot**
When reviewing clients/src code, look for wrong serialization / deserialization patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-19471: enable acknowledgement for a record which could not be deserialized (#20148).

**Test**
Test modified in ShareConsumerTest.java covering the corrected behaviour.
