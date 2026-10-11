**Commit**: 75052bb8f5 · **Ticket**: KAFKA-20422
**Subsystem**: streams/integration-tests
**Category**: Logic error in condition

**Root cause**
Add integration tests for DSL operations with header stores via
Materialized.as(). Reviewers: Matthias J. Sax <matthias@confluent.io>

**Before/After**
```java
// Before
// (no removals)

// After
import org.apache.kafka.common.serialization.LongDeserializer;
import org.apache.kafka.streams.kstream.TimeWindowedDeserializer;
import org.apache.kafka.streams.kstream.WindowedSerdes;
import org.apache.kafka.streams.state.AggregationWithHeaders;
import org.apache.kafka.streams.state.SessionStoreWithHeaders;

    @Test
    public void processorShouldAccessKStreamAggregatedKTableStoreAsHeadersStoreViaSupplier() {
        final StreamsBuilder builder = new StreamsBuilder();

        final Materialized<String, String, KeyValueStore<Bytes, byte[]>> materialized =
            Materialized.as(Stores.persistentTimestampedKeyValueStoreWithHeaders("table-store"));
```

**How to spot**
When reviewing streams/integration-tests code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-20422: add dsl integration tests for header stores with explicit suppliers (#22002).

**Test**
Test modified in PapiDslIntegrationTest.java covering the corrected behaviour.
