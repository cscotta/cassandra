**Commit**: 1bf7d6032b · **Ticket**: KAFKA-20173
**Subsystem**: streams/src
**Category**: Wrong serialization / deserialization

**Root cause**
Ensures that `ChangedSerde`,  `SubscriptionWrapperSerde` and
`ValueAndTimestampSerde` forward headers to their underlying serdes. Reviewers: Matthias J. Sax <matthias@confluent.io>

**Before/After**
```java
// Before
        return deserialize(topic, null, data);
        return serialize(topic, null, data);
        public byte[] serialize(final String ignored, final SubscriptionWrapper<KLeft> data) {
                return serializeV0(data);
                return serializeV1(data);
        private byte[] serializePrimaryKey(final SubscriptionWrapper<KLeft> data) {
        private ByteBuffer serializeCommon(final SubscriptionWrapper<KLeft> data, final byte version, final int extraLength) {
            final byte[] primaryKeySerializedData = serializePrimaryKey(data);
        private byte[] serializeV0(final SubscriptionWrapper<KLeft> data) {
            return serializeCommon(data, (byte) 0, 0).array();
        private byte[] serializeV1(final SubscriptionWrapp

// After
        throw new UnsupportedOperationException("ChangedDeserializer requires the headers-aware version of deserialize");
        throw new UnsupportedOperationException("ChangedSerializer requires the headers-aware version of serialize");
import org.apache.kafka.common.header.Headers;
        public byte[] serialize(final String ignored, final Headers headers, final SubscriptionWrapper<KLeft> data) {
                return serializeV0(data, headers);
                return serializeV1(data, headers);
        @Override
        public byte[] serialize(final String ignored, final SubscriptionWrapper<KLeft> data) {
            throw new UnsupportedOperationException("SubscriptionWrapperSerializer requires the headers-aware version of serialize");
        }

        private byte[] serializePrimaryKey(final SubscriptionWrapper<KLeft> data, final Headers headers) {
```

**How to spot**
When reviewing streams/src code, look for wrong serialization / deserialization patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-20173: propagate headers into serde 8/n  (#21967).

**Test**
Test modified in ChangedSerdeTest.java covering the corrected behaviour.
