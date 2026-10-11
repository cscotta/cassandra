**Commit**: 75d26f2717 · **Ticket**: KAFKA-20127
**Subsystem**: streams/src
**Category**: Logic error in condition

**Root cause**
This PR is part of implementation of KIP-1271, adding new methods to
enable storing headers in state store. Reviewers: Alieh Saeedi <asaeedi@confluent.io>,  Matthias J. Sax
 <matthias@confluent.io>

**Before/After**
```java
// Before
     * @param plainValue the legacy timestamped value bytes
    static byte[] convertToHeaderFormat(final byte[] plainValue) {
        if (plainValue == null) {
            .allocate(1 + plainValue.length)
            .put(plainValue)
        return keySerde.deserializer().deserialize(topic, rawKey);
        return valueSerde.deserializer().deserialize(topic, rawValue);
            return keySerde.serializer().serialize(topic, key);
                    String.format("A serializer (%s) is not compatible to the actual key type " +
                                    "(key type: %s). Change the default Serdes in StreamConfig or " +
                                    "provide correct Serdes via method parameters.",
                            keySerializer().getClass().getName(),

// After
     * @param valueAndTimestamp the legacy timestamped value bytes
    static byte[] convertToHeaderFormat(final byte[] valueAndTimestamp) {
        if (valueAndTimestamp == null) {
            .allocate(1 + valueAndTimestamp.length)
            .put(valueAndTimestamp)
import org.apache.kafka.common.header.Headers;
import org.apache.kafka.common.header.internals.RecordHeaders;
     * @deprecated Since 4.3. Use {@link #keyFrom(byte[], Headers)} instead.
    @Deprecated
        return keyFrom(rawKey, new RecordHeaders());
    }

```

**How to spot**
When reviewing streams/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-20127: add new methods to stateserdes (#21411).

**Test**
Test modified in StateSerdesTest.java covering the corrected behaviour.
