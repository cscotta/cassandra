**Commit**: 7bd979bb4f · **Ticket**: KAFKA-20173
**Subsystem**: checkstyle/import-control.xml
**Category**: Wrong serialization / deserialization

**Root cause**
Follow-up for:
- https://github.com/apache/kafka/pull/21490
- https://github.com/apache/kafka/pull/21536

This PR fixes headers propagation in `ListSerde`. Reviewers: Matthias J. Sax <matthias@confluent.io>

**Before/After**
```java
// Before
                deserializedList.add(inner.deserialize(topic, payload));
                    final byte[] bytes = inner.serialize(topic, entry);

// After
      <allow class="org.apache.kafka.common.header.internals.RecordHeaders" />
import org.apache.kafka.common.header.Headers;
import org.apache.kafka.common.header.internals.RecordHeaders;
        return deserialize(topic, new RecordHeaders(), data);
    }

    @Override
    public List<Inner> deserialize(String topic, Headers headers, byte[] data) {
                deserializedList.add(inner.deserialize(topic, headers, payload));
import org.apache.kafka.common.header.Headers;
import org.apache.kafka.common.header.internals.RecordHeaders;
        return serialize(topic, new RecordHeaders(), data);
```

**How to spot**
When reviewing checkstyle/import-control.xml code, look for wrong serialization / deserialization patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-20173: propagate headers into serde 3/n (#21681).

**Test**
Test modified in ListDeserializerTest.java covering the corrected behaviour.
