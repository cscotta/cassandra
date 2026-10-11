**Commit**: b2dd7df159 · **Ticket**: KAFKA-20202
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
// (no removals)

// After
import org.apache.kafka.streams.kstream.Windowed;
import org.apache.kafka.streams.query.FailureReason;
import org.apache.kafka.streams.query.PositionBound;
import org.apache.kafka.streams.query.Query;
import org.apache.kafka.streams.query.QueryConfig;
import org.apache.kafka.streams.query.QueryResult;
import org.apache.kafka.streams.query.WindowKeyQuery;
import org.apache.kafka.streams.query.WindowRangeQuery;
import org.apache.kafka.streams.query.internals.InternalQueryResultUtil;
import org.apache.kafka.streams.state.KeyValueIterator;
import org.apache.kafka.streams.state.TimestampedBytesStore;
import org.apache.kafka.streams.state.ValueAndTimestamp;
```

**How to spot**
When reviewing streams/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-20202: verify existing iqv2 methods for meteredtimestampedwindowstorewithheaders (#21650).

**Test**
Test modified in InMemoryWindowStoreTest.java covering the corrected behaviour.
