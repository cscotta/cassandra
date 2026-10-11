**Commit**: d920f8bc1f · **Ticket**: KAFKA-10863
**Subsystem**: clients/src
**Category**: Wrong serialization / deserialization

**Root cause**
Modified From #19110

1. Convert ControlRecordType schema to use auto-generated protocol
`ControlRecordTypeSchema`. 2. Substitute `CURRENT_CONTROL_RECORD_KEY_SIZE` with
`controlRecordKeySize()` method since the size is accumulated from
`ControlRecordTypeSchema`.

**Before/After**
```java
// Before
import org.apache.kafka.common.protocol.types.Field;
import org.apache.kafka.common.protocol.types.Schema;
import org.apache.kafka.common.protocol.types.Struct;
import org.apache.kafka.common.protocol.types.Type;

    static final short CURRENT_CONTROL_RECORD_KEY_VERSION = 0;
    static final int CURRENT_CONTROL_RECORD_KEY_SIZE = 4;
    private static final Schema CONTROL_RECORD_KEY_SCHEMA_VERSION_V0 = new Schema(
            new Field("version", Type.INT16),
            new Field("type", Type.INT16));
    public Struct recordKey() {
        Struct struct = new Struct(CONTROL_RECORD_KEY_SCHEMA_VERSION_V0);

// After
import org.apache.kafka.common.message.ControlRecordTypeSchema;
import org.apache.kafka.common.protocol.ByteBufferAccessor;
import org.apache.kafka.common.protocol.MessageUtil;
    private static final int CONTROL_RECORD_KEY_SIZE = 4;
    private final ByteBuffer buffer;
        ControlRecordTypeSchema schema = new ControlRecordTypeSchema().setType(type);
        buffer = MessageUtil.toVersionPrefixedByteBuffer(ControlRecordTypeSchema.HIGHEST_SUPPORTED_VERSION, schema);
    public ByteBuffer recordKey() {
        return buffer.duplicate();
    }
    public int controlRecordKeySize() {
        return buffer.remaining();
```

**How to spot**
When reviewing clients/src code, look for wrong serialization / deserialization patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-10863 convert controrecordtype schema to use auto-generated protocol (#21439).

**Test**
Test modified in ControlRecordTypeTest.java covering the corrected behaviour.
