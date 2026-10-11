**Commit**: 8b6d31f00c · **Ticket**: KAFKA-20297
**Subsystem**: clients/src
**Category**: Logic error in condition

**Root cause**
Summary: moves the testIncrement and testIncrementUpperBoundary test
methods from BytesTest.java to ByteUtilsTest.java. Reviewers: Ken Huang <s7133700@gmail.com>, Chia-Ping Tsai
 <chia7712@gmail.com>

**Before/After**
```java
// Before
import org.apache.kafka.common.utils.internals.ByteUtils;

import java.nio.charset.StandardCharsets;
import java.util.Comparator;
import java.util.NavigableMap;
import java.util.TreeMap;


    public void testIncrement() {
        byte[] input = new byte[]{(byte) 0xAB, (byte) 0xCD, (byte) 0xFF};
        byte[] expected = new byte[]{(byte) 0xAB, (byte) 0xCE, (byte) 0x00};
        Bytes output = ByteUtils.increment(Bytes.wrap(input));

// After
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
    public void equalsReflectsContentIdentityAndType() {
        Bytes a = Bytes.wrap(new byte[]{0x01, 0x02, 0x03});
        Bytes b = Bytes.wrap(new byte[]{0x01, 0x02, 0x03});
        Bytes c = Bytes.wrap(new byte[]{0x01, 0x02, 0x04});
        Bytes d = Bytes.wrap(new byte[]{0x01, 0x02});
        Bytes empty1 = Bytes.wrap(new byte[]{});
        Bytes empty2 = Bytes.wrap(new byte[]{});

        assertEquals(a, a);
```

**How to spot**
When reviewing clients/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-20297 move testincrement and testincrementupperboundary to byteutilstest (#21913).

**Test**
Test modified in BytesTest.java covering the corrected behaviour.
