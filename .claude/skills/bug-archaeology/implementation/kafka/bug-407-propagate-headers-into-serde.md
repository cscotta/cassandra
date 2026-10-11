**Commit**: 0025e0757f · **Ticket**: KAFKA-20173
**Subsystem**: streams/src
**Category**: Wrong serialization / deserialization

**Root cause**
Follow-up for https://github.com/apache/kafka/pull/21490

This PR concentrate on `WindowKeySchema` and related classes:
- `PrefixedWindowKeySchemas`
- `TimeWindowedSerializer`
- `TimeWindowedDeserializer`

Reviewers: Alieh Saeedi <asaeedi@confluent.io>, Matthias J. Sax
 <matthias@confluent.io>

**Before/After**
```java
// Before
            return WindowKeySchema.fromStoreKey(data, windowSize, inner, topic);
        return WindowKeySchema.from(data, windowSize, inner, topic);
        return WindowKeySchema.toBinary(data, inner, topic);
        return storeKeyToWindowKey.toWindowKey(binaryKey, windowSize, serdes.keyDeserializer(), serdes.topic());
        Windowed<Bytes> toWindowKey(final byte[] binaryKey, final long windowSize, final Deserializer<Bytes> deserializer, final String topic);

// After
import org.apache.kafka.common.header.Headers;
import org.apache.kafka.common.header.internals.RecordHeaders;
        return deserialize(topic, new RecordHeaders(), data);
    }

    @Override
    public Windowed<T> deserialize(final String topic, final Headers headers, final byte[] data) {
            return WindowKeySchema.fromStoreKey(data, windowSize, inner, headers, topic);
        return WindowKeySchema.from(data, windowSize, inner, headers, topic);
import org.apache.kafka.common.header.Headers;
import org.apache.kafka.common.header.internals.RecordHeaders;
        return serialize(topic, new RecordHeaders(), data);
```

**How to spot**
When reviewing streams/src code, look for wrong serialization / deserialization patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-20173: propagate headers into serde 2/n (#21536).

**Test**
Test modified in SessionWindowedDeserializerTest.java covering the corrected behaviour.
