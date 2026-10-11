**Commit**: ef14f76fb3 · **Ticket**: KAFKA-6629
**Subsystem**: streams/src
**Category**: Wrong serialization / deserialization

**Root cause**
Addresses:
[KAFKA-6629](https://issues.apache.org/jira/browse/KAFKA-6629)

Adds configuration for the SessionKeySchema and parameterises the
existing tests  so that both WindowKeys and SessionKeys are tested under
the existing unit tests. Reviewers: Bill Bejeck <bbejeck@apache.org>

---------

Co-authored-by: Lorcan <lorcanjames1@gmail.com>

**Before/After**
```java
// Before
import org.junit.jupiter.api.Test;
// TODO: this test coverage does not consider session serde yet
public class SegmentedCacheFunctionTest {
    private static final int TIMESTAMP = 736213517;
    private static final Bytes THE_KEY = WindowKeySchema.toStoreKeyBinary(new byte[]{0xA, 0xB, 0xC}, TIMESTAMP, 42);
    private static final Bytes THE_CACHE_KEY = Bytes.wrap(
        ByteBuffer.allocate(8 + THE_KEY.get().length)
            .putLong(TIMESTAMP / SEGMENT_INTERVAL)
            .put(THE_KEY.get()).array()
    private final SegmentedCacheFunction cacheFunction = new SegmentedCacheFunction(new WindowKeySchema(), SEGMENT_INTERVAL);
    @Test
    public void key() {

// After
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.stream.Stream;
import static org.apache.kafka.streams.state.StateSerdes.TIMESTAMP_SIZE;
class SegmentedCacheFunctionTest {
    private static final int START_TIMESTAMP = 736213517;
    private static final int END_TIMESTAMP = 800000000;
    private static final Bytes THE_WINDOW_KEY = WindowKeySchema.toStoreKeyBinary(new byte[]{0xA, 0xB, 0xC}, START_TIMESTAMP, 42);
    private static final Bytes THE_SESSION_KEY = toStoreKeyBinary(new byte[]{0xA, 0xB, 0xC}, END_TIMESTAMP, START_TIMESTAMP);

    private static final Bytes THE_WINDOW_CACHE_KEY = Bytes.wrap(
```

**How to spot**
When reviewing streams/src code, look for wrong serialization / deserialization patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-6629: parameterise segmentedcachefunctiontest for session key schemas (#19404).

**Test**
Test modified in SegmentedCacheFunctionTest.java covering the corrected behaviour.
