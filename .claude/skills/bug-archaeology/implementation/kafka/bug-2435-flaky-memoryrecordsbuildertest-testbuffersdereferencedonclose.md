**Commit**: 190df07ace · **Ticket**: KAFKA-17265
**Subsystem**: clients/src
**Category**: Missing null / bounds check

**Root cause**
Reviewers: TaiJuWu <tjwu1217@gmail.com>, Chia-Ping Tsai <chia7712@gmail.com>

**Before/After**
```java
// Before
import java.util.Random;
    public void testBuffersDereferencedOnClose(Args args) {
        Runtime runtime = Runtime.getRuntime();
        int payloadLen = 1024 * 1024;
        ByteBuffer buffer = ByteBuffer.allocate(payloadLen * 2);
        byte[] key = new byte[0];
        byte[] value = new byte[payloadLen];
        new Random().nextBytes(value); // Use random payload so that compressed buffer is large
        List<MemoryRecordsBuilder> builders = new ArrayList<>(100);
        long startMem = 0;
        long memUsed = 0;
        int iterations =  0;

// After
    public void shouldThrowIllegalStateExceptionOnAppendWhenClosed(Args args) {
        ByteBuffer buffer = allocateBuffer(128, args);
        MemoryRecordsBuilder builder = new MemoryRecordsBuilder(buffer, args.magic, args.compression,
                TimestampType.CREATE_TIME, 0L, 0L, RecordBatch.NO_PRODUCER_ID,
                RecordBatch.NO_PRODUCER_EPOCH, RecordBatch.NO_SEQUENCE, false, false,
                RecordBatch.NO_PARTITION_LEADER_EPOCH, buffer.capacity());
        builder.append(0L, "a".getBytes(), "1".getBytes());
        builder.build();

        assertEquals("Tried to append a record, but MemoryRecordsBuilder is closed for record appends",
                assertThrows(IllegalStateException.class, () -> builder.append(0L, "a".getBytes(), "1".getBytes())).getMessage());
```

**How to spot**
When reviewing clients/src code, look for missing null / bounds check patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-17265 fix flaky memoryrecordsbuildertest#testbuffersdereferencedonclose (#17092).

**Test**
Test modified in MemoryRecordsBuilderTest.java covering the corrected behaviour.
