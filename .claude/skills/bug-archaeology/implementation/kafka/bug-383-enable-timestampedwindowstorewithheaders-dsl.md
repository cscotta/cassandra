**Commit**: 1e7a4852c1 · **Ticket**: KAFKA-20221
**Subsystem**: streams/src
**Category**: Logic error in condition

**Root cause**
This PR enables headers-aware window stores in the Kafka Streams DSL,
implementing the foundational infrastructure for KIP-1285. Reviewers: Alieh Saeedi <asaeedi@confluent.io>, Matthias J. Sax
 <matthias@confluent.io>

**Before/After**
```java
// Before
import org.apache.kafka.streams.state.TimestampedWindowStore;
                        true
        final StoreBuilder<TimestampedWindowStore<K, V>> builder = Stores
                .timestampedWindowStoreBuilder(
                        supplier,
                        materialized.keySerde(),
                        materialized.valueSerde()
                );
                        false
import org.apache.kafka.streams.state.TimestampedWindowStore;

// After
import org.apache.kafka.streams.DslStoreFormat;
        final DslStoreFormat storeFormat = dslStoreFormat() == null ? DslStoreFormat.TIMESTAMPED : dslStoreFormat();
                        storeFormat
        final StoreBuilder<?> builder;
        if (storeFormat == DslStoreFormat.HEADERS) {
            builder = Stores.timestampedWindowStoreWithHeadersBuilder(
                    supplier,
                    materialized.keySerde(),
                    materialized.valueSerde());
        } else {
            builder = Stores.timestampedWindowStoreBuilder(
                    supplier,
```

**How to spot**
When reviewing streams/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-20221: enable timestampedwindowstorewithheaders in dsl (1/n) (#21599).

**Test**
Test modified in KStreamWindowAggregateTest.java covering the corrected behaviour.
