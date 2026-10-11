**Commit**: a4752b4b83 · **Ticket**: KAFKA-20185
**Subsystem**: streams/src
**Category**: Logic error in condition

**Root cause**
This PR is NOT about implementing IQv2s for header-store, but provides
IQv2s for headers store through the timestamped state stores. So the
results do not contain the headers even though the headers are preserved
in the headers state store. Part of KIP-1271. Reviewers: Matthias J.

**Before/After**
```java
// Before

        if (cacheType == CacheType.TIMESTAMPED_KEY_VALUE_STORE_WITH_HEADERS) {
            throw new UnsupportedOperationException("Queries (IQv2) are not supported for timestamped key-value stores with headers yet.");
        }

        if (cacheType == CacheType.TIMESTAMPED_KEY_VALUE_STORE_WITH_HEADERS) {
            throw new UnsupportedOperationException("Queries (IQv2) are not supported for timestamped key-value stores with headers yet.");
        }
import org.apache.kafka.streams.query.Position;
                         final ValueTimestampHeaders<V> value) {

// After
import org.apache.kafka.common.metrics.Sensor;
import org.apache.kafka.streams.KeyValue;
import org.apache.kafka.streams.query.KeyQuery;
import org.apache.kafka.streams.query.RangeQuery;
import org.apache.kafka.streams.query.ResultOrder;
import org.apache.kafka.streams.query.TimestampedKeyQuery;
import org.apache.kafka.streams.query.TimestampedRangeQuery;
import org.apache.kafka.streams.query.internals.InternalQueryResultUtil;
import org.apache.kafka.streams.state.KeyValueIterator;
import org.apache.kafka.streams.state.ValueAndTimestamp;
import java.util.Map;
import java.util.function.Function;
```

**How to spot**
When reviewing streams/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-20185: verify existing iqv2 methods for meteredtimestampedkeyvaluestorewithheaders (#21643).

**Test**
Test modified in RocksDBTimestampedStoreWithHeadersTest.java covering the corrected behaviour.
