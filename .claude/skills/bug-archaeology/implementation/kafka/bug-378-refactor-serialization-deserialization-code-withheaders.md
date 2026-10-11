**Commit**: 10d52734dc · **Ticket**: KAFKA-20205
**Subsystem**: streams/src
**Category**: Wrong serialization / deserialization

**Root cause**
This patch refactors several common functions in the package
`streams.state.internals`  into a utility class `Utils`. According to the JIRA description:
- all repetitive methods such as
`readBytes` in de/serializaters of *WithHeaders types (ref:
https://github.com/apache/kafka/pull/21511/changes#r2832393068)
 - `keyBytes` methods in metered classes across all state store types. - `rawTimestampedValue` methods in the in adapter classes across all
state store types. Test:
- Original functions already covered by unit tests, so simply redirect
the code path in prior test to the new functions
- Specific test cases are added for the new functions

Reviewers: TengYao Chi <frankvicky@apache.org>

**Before/After**
```java
// Before
import org.apache.kafka.common.errors.SerializationException;

    /**
     * Reads the specified number of bytes from the buffer with validation.
     *
     * @param buffer the ByteBuffer to read from
     * @param length the number of bytes to read
     * @return the byte array containing the read bytes
     * @throws SerializationException if buffer doesn't have enough bytes or length is negative
     */
    private static byte[] readBytes(final ByteBuffer buffer, final int length) {
        if (length < 0) {

// After
import static org.apache.kafka.streams.state.internals.Utils.readBytes;
}
import static org.apache.kafka.streams.state.internals.Utils.readBytes;

            final byte[] keyBytes = readBytes(buffer, keyLength);

                value = readBytes(buffer, valueLength);
import static org.apache.kafka.streams.state.internals.Utils.keyBytes;
```

**How to spot**
When reviewing streams/src code, look for wrong serialization / deserialization patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-20205: refactor serialization and deserialization code for *withheaders types (#21626).

**Test**
Test modified in UtilsTest.java covering the corrected behaviour.
