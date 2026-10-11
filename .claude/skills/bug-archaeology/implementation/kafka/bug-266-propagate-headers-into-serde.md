**Commit**: 9426f93c47 · **Ticket**: KAFKA-20173
**Subsystem**: streams/src
**Category**: Wrong serialization / deserialization

**Root cause**
This PR ensures headers are propagated in
`SubscriptionResponseWrapperSerde`,
`LeftOrRightValueSerializer/Deserializer` and
`TimestampedKeyAndJoinSideSerializer/Deserializer`  Added unit tests to
check headers propagation. Reviewers: Murali Basani <muralidhar.basani@aiven.io>, Alieh Saeedi
 <asaeedi@confluent.io>, Matthias J. Sax <matthias@confluent.io>

**Before/After**
```java
// Before
            final byte[] serializedData = data.foreignValue() == null ? null : serializer.serialize(topic, data.foreignValue());
                value = deserializer.deserialize(topic, serializedValue);
            ? LeftOrRightValue.makeLeftValue(leftDeserializer.deserialize(topic, rawValue(data)))
            : LeftOrRightValue.makeRightValue(rightDeserializer.deserialize(topic, rawValue(data)));

// After
import org.apache.kafka.common.header.Headers;
            throw new UnsupportedOperationException("SubscriptionResponseWrapperSerializer requires the headers-aware version of serialize");
        }

        @Override
        public byte[] serialize(final String topic, final Headers headers, final SubscriptionResponseWrapper<V> data) {
            final byte[] serializedData = data.foreignValue() == null ? null : serializer.serialize(topic, headers, data.foreignValue());
            throw new UnsupportedOperationException("SubscriptionResponseWrapperSerializer requires the headers-aware version of deserialize");
        }

        @Override
        public SubscriptionResponseWrapper<V> deserialize(final String topic, final Headers headers, final byte[] data) {
```

**How to spot**
When reviewing streams/src code, look for wrong serialization / deserialization patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-20173: propagate headers into serde 7/n (#21851).

**Test**
Test modified in SubscriptionResponseWrapperSerdeTest.java covering the corrected behaviour.
