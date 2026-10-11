**Commit**: 0013d78298 · **Ticket**: KAFKA-19330
**Subsystem**: clients/src
**Category**: Wrong serialization / deserialization

**Root cause**
While rewriting `EndToEndClusterIdTest` in Java (#19741 ), I found that
the test uses `MockInterceptor` and `MockSerializer` together. However,
`MockSerializer` was using a `byte[]` serializer, while
`MockInterceptor` expected a `String` serializer, leading to a
`ClassCastException`. I chose to update `MockSerializer` to use `String`, as it is used less
frequently than the `MockInterceptor`. Using `String` also simplifies
the code  by avoiding the need to write expressions like
`"value".getBytes`.

**Before/After**
```java
// Before
        KafkaProducer<byte[], byte[]> producer = new KafkaProducer<>(
                configs, new MockSerializer(), new MockSerializer());
        assertEquals(oldInitCount + 2, MockSerializer.INIT_COUNT.get());
        assertEquals(oldCloseCount, MockSerializer.CLOSE_COUNT.get());
        producer.close();
    private MockProducer<byte[], byte[]> producer;
    private final ProducerRecord<byte[], byte[]> record1 = new ProducerRecord<>(topic, "key1".getBytes(), "value1".getBytes());
    private final ProducerRecord<byte[], byte[]> record2 = new ProducerRecord<>(topic, "key2".getBytes(), "value2".getBytes());
        List<ProducerRecord<byte[], byte[]>> expectedResult = new ArrayList<>();
        List<ProducerRecord<byte[], byte[]>> expectedResult = new ArrayList<>();
public class MockDeserializer implements ClusterResourceListener, Deserializer<byte[]> {
    public byte[] deserialize(String topic, byte[] data) {

// After
        try (var ignored = new KafkaProducer<>(configs, new MockSerializer(), new MockSerializer())) {
            assertEquals(oldInitCount + 2, MockSerializer.INIT_COUNT.get());
            assertEquals(oldCloseCount, MockSerializer.CLOSE_COUNT.get());
        }
    private MockProducer<String, String> producer;
    private final ProducerRecord<String, String> record1 = new ProducerRecord<>(topic, "key1", "value1");
    private final ProducerRecord<String, String> record2 = new ProducerRecord<>(topic, "key2", "value2");
        List<ProducerRecord<String, String>> expectedResult = new ArrayList<>();
        List<ProducerRecord<String, String>> expectedResult = new ArrayList<>();
import java.nio.charset.StandardCharsets;
public class MockDeserializer implements ClusterResourceListener, Deserializer<String> {
    public String deserialize(String topic, byte[] data) {
```

**How to spot**
When reviewing clients/src code, look for wrong serialization / deserialization patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-19330 change mockserializer/deserializer to use string serializer instead of byte[] (#19812).

**Test**
Test modified in KafkaProducerTest.java covering the corrected behaviour.
