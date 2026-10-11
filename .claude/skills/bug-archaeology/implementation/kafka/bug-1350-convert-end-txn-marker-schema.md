**Commit**: 1bfa4cd17b · **Ticket**: KAFKA-10864
**Subsystem**: clients/src
**Category**: Wrong serialization / deserialization

**Root cause**
1. Convert end txn marker schema to use auto-generated protocol`EndTxnMarker`
2. substitute `CURRENT_END_TXN_MARKER_VALUE_SIZE` with an`endTnxMarkerValueSize` method since the size is accumulated from `EndTxnMarker`. 3.

**Before/After**
```java
// Before
import org.apache.kafka.common.protocol.types.Field;
import org.apache.kafka.common.protocol.types.Schema;
import org.apache.kafka.common.protocol.types.Struct;
import org.apache.kafka.common.protocol.types.Type;
    private static final short CURRENT_END_TXN_MARKER_VERSION = 0;
    private static final Schema END_TXN_MARKER_SCHEMA_VERSION_V0 = new Schema(
            new Field("version", Type.INT16),
            new Field("coordinator_epoch", Type.INT32));
    static final int CURRENT_END_TXN_MARKER_VALUE_SIZE = 6;
    static final int CURRENT_END_TXN_SCHEMA_RECORD_SIZE = DefaultRecord.sizeInBytes(0, 0L,
            ControlRecordType.CURRENT_CONTROL_RECORD_KEY_SIZE,
            EndTransactionMarker.CURRENT_END_TXN_MARKER_VALUE_SIZE,

// After
import org.apache.kafka.common.message.EndTxnMarker;
import org.apache.kafka.common.protocol.ByteBufferAccessor;
import org.apache.kafka.common.protocol.MessageUtil;
    private final ByteBuffer buffer;
        EndTxnMarker marker = new EndTxnMarker().setCoordinatorEpoch(coordinatorEpoch);
        this.buffer = MessageUtil.toVersionPrefixedByteBuffer(EndTxnMarker.HIGHEST_SUPPORTED_VERSION, marker);
        return buffer.duplicate();
            throw new IllegalArgumentException("Invalid control record type for end transaction marker " + type);
    // Visible for testing
        short version = value.getShort();
        if (version < EndTxnMarker.LOWEST_SUPPORTED_VERSION)
        if (version > EndTxnMarker.HIGHEST_SUPPORTED_VERSION)
```

**How to spot**
When reviewing clients/src code, look for wrong serialization / deserialization patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-10864 convert end txn marker schema to use auto-generated protocol (#9766).

**Test**
Test modified in EndTransactionMarkerTest.java covering the corrected behaviour.
