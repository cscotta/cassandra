**Commit**: 448338cab5 · **Ticket**: KAFKA-20173
**Subsystem**: streams/src
**Category**: Wrong serialization / deserialization

**Root cause**
Propagate headers into serializes / deserializers

This PR partially covers `org.apache.kafka.streams.kstream` package. Code:
- Use empty headers for backward compatibility
- Propagate headers when it's possible

Tests:
- Use empty headers for existing tests
- Add new test if needed

Reviewers: Matthias J. Sax <matthias@confluent.io>, Alieh Saeedi
 <asaeedi@confluent.io>, TengYao Chi <frankvicky@apache.org>

---------

Co-authored-by: Matthias J. Sax <mjsax@apache.org>

**Before/After**
```java
// Before
        return SessionKeySchema.from(data, inner, topic);
        return SessionKeySchema.toBinary(data, inner, topic);
                    record.withKey(SessionKeySchema.from(record.key(), serdes.keyDeserializer(), serdes.topic()))

// After
import org.apache.kafka.common.header.Headers;
import org.apache.kafka.common.header.internals.RecordHeaders;
        return deserialize(topic, new RecordHeaders(), data);
    }

    @Override
    public Windowed<T> deserialize(final String topic, final Headers headers, final byte[] data) {
        return SessionKeySchema.from(data, inner, headers, topic);
import org.apache.kafka.common.header.Headers;
import org.apache.kafka.common.header.internals.RecordHeaders;
        return serialize(topic, new RecordHeaders(), data);
    }
```

**How to spot**
When reviewing streams/src code, look for wrong serialization / deserialization patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-20173: propagate headers into serde 1/n (#21490).

**Test**
Test modified in SessionWindowedDeserializerTest.java covering the corrected behaviour.
