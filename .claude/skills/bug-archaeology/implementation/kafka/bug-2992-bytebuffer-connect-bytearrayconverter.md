**Commit**: 32b2b73f67 · **Ticket**: KAFKA-16844
**Subsystem**: connect/runtime
**Category**: Logic error in condition

**Root cause**
Reviewers: Chris Egerton <chrise@aiven.io>

**Before/After**
```java
// Before
        if (value != null && !(value instanceof byte[]))
        return (byte[]) value;

// After
import java.nio.ByteBuffer;
        if (value != null && !(value instanceof byte[]) && !(value instanceof ByteBuffer))
        return value instanceof ByteBuffer ? getBytesFromByteBuffer((ByteBuffer) value) : (byte[]) value;

    private byte[] getBytesFromByteBuffer(ByteBuffer byteBuffer) {
        if (byteBuffer == null) {
            return null;
        }

        byteBuffer.rewind();
        byte[] bytes = new byte[byteBuffer.remaining()];
        byteBuffer.get(bytes);
```

**How to spot**
When reviewing connect/runtime code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-16844: add bytebuffer support for connect bytearrayconverter (#16101).

**Test**
Test modified in ByteArrayConverterTest.java covering the corrected behaviour.
