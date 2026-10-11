**Commit**: d67379c310 · **Ticket**: KAFKA-18301
**Subsystem**: build.gradle
**Category**: Wrong serialization / deserialization

**Root cause**
This patch is the first one in a series to improve how coordinator records are managed. It focuses on making coordinator records first class citizen in the generator. * Introduce `coordinator-key` and `coordinator-value` in the schema;
* Introduce `apiKey` for those. This is done to avoid relying on the version to determine the type.

**Before/After**
```java
// Before
             "-m", "MessageDataGenerator", "JsonConverterGenerator"
        // take the inner array directly if it is full with data
                buffer.arrayOffset() == 0 &&
                buffer.position() == 0 &&
                buffer.limit() == buffer.array().length) return buffer.array();
import org.apache.kafka.coordinator.group.generated.{GroupMetadataValue, OffsetCommitKey, OffsetCommitValue, GroupMetadataKey => GroupMetadataKeyData}
    MessageUtil.toVersionPrefixedBytes(OffsetCommitKey.HIGHEST_SUPPORTED_VERSION,

// After
             "-m", "MessageDataGenerator", "JsonConverterGenerator",
             "-t", "CoordinatorRecordTypeGenerator"
        // take the inner array directly if it is full of data.
            buffer.arrayOffset() == 0 &&
            buffer.position() == 0 &&
            buffer.limit() == buffer.array().length) return buffer.array();
        else return Utils.toArray(buffer);
    }

    public static ByteBuffer toCoordinatorTypePrefixedByteBuffer(final short type, final Message message) {
        if (message.highestSupportedVersion() != 0 || message.lowestSupportedVersion() != 0) {
            throw new IllegalArgumentException("Cannot serialize a message with a different version than 0.");
```

**How to spot**
When reviewing build.gradle code, look for wrong serialization / deserialization patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-18301; make coordinator records first class citizen (#18261).

**Test**
Test modified in GroupMetadataManagerTest.scala covering the corrected behaviour.
