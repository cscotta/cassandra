**Commit**: 13df7ed595 · **Ticket**: KAFKA-20308
**Subsystem**: streams/test-utils
**Category**: Logic error in condition

**Root cause**
This PR adds full TopologyTestDriver support for headers-aware state
stores, enabling tests to both access headers and use the simple
non-headers API automatically. Reviewers: Matthias J. Sax <matthias@confluent.io>

**Before/After**
```java
// Before
// (no removals)

// After
import org.apache.kafka.streams.query.PositionBound;
import org.apache.kafka.streams.query.Query;
import org.apache.kafka.streams.query.QueryConfig;
import org.apache.kafka.streams.query.QueryResult;
        if (store instanceof TimestampedKeyValueStoreWithHeaders) {
            log.warn("Method #getTimestampedKeyValueStore() should be used to access a TimestampedKeyValueStoreWithHeaders.");
            return new KeyValueStoreFacadeForHeaders<>((TimestampedKeyValueStoreWithHeaders<K, V>) store);
        }
        if (store instanceof TimestampedKeyValueStoreWithHeaders) {
            return new TimestampedKeyValueStoreFacadeForHeaders<>((TimestampedKeyValueStoreWithHeaders<K, V>) store);
        }
        if (store instanceof TimestampedWindowStoreWithHeaders) {
```

**How to spot**
When reviewing streams/test-utils code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-20308: update topologytestdriver to support for headers-aware stores (#21745).

**Test**
Test modified in TopologyTestDriver.java covering the corrected behaviour.
