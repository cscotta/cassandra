**Commit**: 7c03c8f3ab · **Ticket**: KAFKA-17056
**Subsystem**: storage/src
**Category**: Wrong serialization / deserialization

**Root cause**
Reviewers: Chia-Ping Tsai <chia7712@gmail.com>

**Before/After**
```java
// Before
import org.apache.kafka.common.protocol.types.ArrayOf;
import org.apache.kafka.common.protocol.types.Field;
import org.apache.kafka.common.protocol.types.Schema;
import org.apache.kafka.common.protocol.types.SchemaException;
import org.apache.kafka.common.protocol.types.Struct;
import org.apache.kafka.common.protocol.types.Type;
    private static final short PRODUCER_SNAPSHOT_VERSION = 1;
    private static final String VERSION_FIELD = "version";
    private static final String CRC_FIELD = "crc";
    private static final String PRODUCER_ID_FIELD = "producer_id";
    private static final String LAST_SEQUENCE_FIELD = "last_sequence";
    private static final String PRODUCER_EPOCH_FIELD = "epoch";

// After
import org.apache.kafka.common.protocol.ByteBufferAccessor;
import org.apache.kafka.common.protocol.MessageUtil;
import org.apache.kafka.server.log.remote.metadata.storage.generated.ProducerSnapshot;
        byte[] buffer = Files.readAllBytes(file.toPath());
        ByteBuffer byteBuffer = ByteBuffer.wrap(buffer);
        short version;
        ProducerSnapshot producerSnapshot;
        try {
            version = byteBuffer.getShort();
            if (version < ProducerSnapshot.LOWEST_SUPPORTED_VERSION || version > ProducerSnapshot.HIGHEST_SUPPORTED_VERSION)
```

**How to spot**
When reviewing storage/src code, look for wrong serialization / deserialization patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-17056 convert producer state metadata schemas to use generated protocol (#16592).

**Test**
No test added.
