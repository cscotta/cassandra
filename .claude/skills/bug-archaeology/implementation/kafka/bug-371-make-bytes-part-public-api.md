**Commit**: 24202c0d9b · **Ticket**: KAFKA-17939
**Subsystem**: build.gradle
**Category**: Logic error in condition

**Root cause**
### Summary
This PR implements

[KIP-1247](https://cwiki.apache.org/confluence/display/KAFKA/KIP-1247%3A+Make+Bytes+utils+class+part+of+the+public+API)
to officially make the `Bytes` class part of Kafka's public API. ### Changes
- Add `Bytes` to Javadoc generation in `build.gradle`
- Deprecate helper methods that are not part of the intended public API:
  - `increment()`
  - `BYTES_LEXICO_COMPARATOR`
  - `ByteArrayComparator`
- Create `org.apache.kafka.common.utils.internals.BytesUtils` with
duplicates of deprecated helpers
- Update all internal Kafka code to use `BytesUtils` instead of
deprecated methods
- Enhance Javadoc documentation for core `Bytes` methods

### Compatibility
Deprecated ...

**Before/After**
```java
// Before
 * Utility class that handles immutable byte arrays.
        this.bytes = bytes;
        return BYTES_LEXICO_COMPARATOR.compare(this.bytes, that.bytes);
     * Increment the underlying byte array by adding 1. Throws an IndexOutOfBoundsException if incrementing would cause
     * the underlying input byte array to overflow.
        byte[] inputArr = input.get();
        byte[] ret = new byte[inputArr.length];
        int carry = 1;
        for (int i = inputArr.length - 1; i >= 0; i--) {
            if (inputArr[i] == (byte) 0xFF && carry == 1) {
                ret[i] = (byte) 0x00;
            } else {

// After
    include "**/org/apache/kafka/common/utils/Bytes.java"
import org.apache.kafka.common.utils.internals.BytesUtils;

import java.util.Objects;
 * An immutable wrapper for a byte array.
 *
 * <p>This class provides a convenient way to work with byte arrays in Kafka APIs,
 * particularly in Kafka Streams state stores and serialization. It implements
 * {@link Comparable} to enable ordering of byte arrays.
 *
 * <p>The class caches the hashCode for improved performance when used as keys
 * in hash-based data structures.
```

**How to spot**
When reviewing build.gradle code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-17939 make bytes part of the public api (kip-1247) (#21601).

**Test**
Test modified in BytesTest.java covering the corrected behaviour.
