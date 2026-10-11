**Commit**: 0fa536be1d · **Ticket**: KAFKA-20173
**Subsystem**: streams/src
**Category**: Wrong serialization / deserialization

**Root cause**
Ensures that `CombinedKeySchema`,  FK-join processors, and global-store
processor, forward headers to their underlying serdes. Reviewers: Matthias J. Sax <matthias@confluent.io>

**Before/After**
```java
// Before
    Bytes toBytes(final KRight foreignKey, final KLeft primaryKey) {
        final byte[] foreignKeySerializedData = foreignKeySerializer.serialize(foreignKeySerdeTopic,
                                                                               foreignKey);
        final byte[] primaryKeySerializedData = primaryKeySerializer.serialize(primaryKeySerdeTopic,
                                                                               primaryKey);
    public CombinedKey<KRight, KLeft> fromBytes(final Bytes data) {
        final KRight foreignKey = foreignKeyDeserializer.deserialize(foreignKeySerdeTopic, foreignKeyRaw);
        final KLeft primaryKey = primaryKeyDeserializer.deserialize(primaryKeySerdeTopic, primaryKeyRaw);
    Bytes prefixBytes(final KRight key) {
        final byte[] foreignKeySerializedData = foreignKeySerializer.serialize(foreignKeySerdeTopic, key);
            final Bytes prefixBytes = keySchema.prefixBytes(record.key());

// After
import org.apache.kafka.common.header.Headers;
    Bytes toBytes(final KRight foreignKey, final KLeft primaryKey, final Headers headers) {
        final byte[] foreignKeySerializedData = foreignKeySerializer.serialize(
                foreignKeySerdeTopic,
                headers,
                foreignKey
        );
        final byte[] primaryKeySerializedData = primaryKeySerializer.serialize(
                primaryKeySerdeTopic,
                headers,
                primaryKey
        );
```

**How to spot**
When reviewing streams/src code, look for wrong serialization / deserialization patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-20173: propagate headers into serde 9/n  (#21969).

**Test**
Test modified in CombinedKeySchemaTest.java covering the corrected behaviour.
