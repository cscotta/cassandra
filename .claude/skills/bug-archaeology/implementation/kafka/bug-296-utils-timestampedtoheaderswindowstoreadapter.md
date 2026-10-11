**Commit**: 20e681b90d · **Ticket**: KAFKA-20205
**Subsystem**: streams/src
**Category**: Logic error in condition

**Root cause**
https://github.com/apache/kafka/pull/21626 miss the case in
TimestampedToHeadersWindowStoreAdapter. Reviewers: Zheguang Zhao <zheguang.zhao@gmail.com>, TengYao Chi
<frankvicky@apache.org>

Signed-off-by: PoAn Yang <payang@apache.org>

**Before/After**
```java
// Before
import org.apache.kafka.common.utils.ByteUtils;
import java.nio.ByteBuffer;
    /**
     * Extract raw timestamped value (timestamp + value) from serialized ValueTimestampHeaders.
     * This strips the headers portion but keeps timestamp and value intact.
     *
     * Format conversion:
     * Input:  [headersSize(varint)][headers][timestamp(8)][value]
     * Output: [timestamp(8)][value]
     */
    // TODO: should be extract to util class, tracked by KAFKA-20205
    static byte[] rawTimestampedValue(final byte[] rawValueTimestampHeaders) {

// After
import static org.apache.kafka.streams.state.internals.Utils.rawTimestampedValue;
```

**How to spot**
When reviewing streams/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-20205: use utils in timestampedtoheaderswindowstoreadapter (#21886).

**Test**
No test added.
