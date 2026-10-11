**Commit**: 197e5e748a · **Ticket**: KAFKA-20173
**Subsystem**: streams/src
**Category**: Wrong serialization / deserialization

**Root cause**
This PR ensures that FullChangeSerde propagates headers to the wrapped
de/serializers. Reviewers: Matthias J. Sax <matthias@confluent.io>

**Before/After**
```java
// Before
    public Change<byte[]> serializeParts(final String topic, final Change<T> data) {
        final byte[] oldBytes = data.oldValue == null ? null : innerSerializer.serialize(topic, data.oldValue);
        final byte[] newBytes = data.newValue == null ? null : innerSerializer.serialize(topic, data.newValue);
    public Change<T> deserializeParts(final String topic, final Change<byte[]> serialChange) {
            serialChange.oldValue == null ? null : innerDeserializer.deserialize(topic, serialChange.oldValue);
            serialChange.newValue == null ? null : innerDeserializer.deserialize(topic, serialChange.newValue);
                final K key = keySerde.deserializer().deserialize(changelogTopic, next.getKey().key().get());
        final Bytes serializedKey = Bytes.wrap(keySerde.serializer().serialize(changelogTopic, record.key()));
        final Change<byte[]> serialChange = valueSerde.serializeParts(changelogTopic, record.value());

// After
import org.apache.kafka.common.header.Headers;
    public Change<byte[]> serializeParts(final String topic, final Headers headers, final Change<T> data) {
        final byte[] oldBytes = data.oldValue == null ? null : innerSerializer.serialize(topic, headers, data.oldValue);
        final byte[] newBytes = data.newValue == null ? null : innerSerializer.serialize(topic, headers, data.newValue);
    public Change<T> deserializeParts(final String topic, final Headers headers, final Change<byte[]> serialChange) {
            serialChange.oldValue == null ? null : innerDeserializer.deserialize(topic, headers, serialChange.oldValue);
            serialChange.newValue == null ? null : innerDeserializer.deserialize(topic, headers, serialChange.newValue);
                final K key = keySerde.deserializer().deserialize(changelogTopic, context.headers(), next.getKey().key().get());
                    context.headers(),
        final Bytes serializedKey = Bytes.wrap(keySerde.serializer().serialize(changelogTopic, recordContext.headers(), record.key()));
        final Change<byte[]> serialChange = valueSerde.serializeParts(changelogTopic, recordContext.headers(), record.value());
import org.apache.kafka.common.header.Headers;
```

**How to spot**
When reviewing streams/src code, look for wrong serialization / deserialization patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-20173: propagate headers into serde 5/n (#21756).

**Test**
Test modified in FullChangeSerdeTest.java covering the corrected behaviour.
