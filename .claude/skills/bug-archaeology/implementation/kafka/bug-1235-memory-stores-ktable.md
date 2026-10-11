**Commit**: 6af849f864 · **Ticket**: KAFKA-7952
**Subsystem**: streams/src
**Category**: Logic error in condition

**Root cause**
Switch to in-memory story for KTable-KTable left-join test. Reviewers: Matthias J. Sax <matthias@confluent.io>

**Before/After**
```java
// Before
        final StreamsBuilder builder = new StreamsBuilder();
        final StreamsBuilder builder = new StreamsBuilder();
        final StreamsBuilder builder = new StreamsBuilder();
        final StreamsBuilder builder = new StreamsBuilder();
        final KTable<Long, String> one = builder.table(
            tableOne,
            consumed,
            Materialized.as(Stores.inMemoryKeyValueStore("tableOne-base-store")));
        final KTable<Long, String> two = builder.table(
            tableTwo,
            consumed,
            Materialized.as(Stores.inMemoryKeyValueStore("tableTwo-base-store")));

// After
import org.apache.kafka.streams.StreamsConfig;
import org.apache.kafka.streams.TopologyConfig;
import org.apache.kafka.streams.state.BuiltInDslStoreSuppliers;
    private StreamsBuilder createStreamBuilderInMemory() {
        props.put(StreamsConfig.DSL_STORE_SUPPLIERS_CLASS_CONFIG, BuiltInDslStoreSuppliers.InMemoryDslStoreSuppliers.class.getName());
        return new StreamsBuilder(new TopologyConfig(new StreamsConfig(props)));
    }

        final StreamsBuilder builder = createStreamBuilderInMemory();
        final StreamsBuilder builder = createStreamBuilderInMemory();
        final StreamsBuilder builder = createStreamBuilderInMemory();
        final StreamsBuilder builder = createStreamBuilderInMemory();
```

**How to spot**
When reviewing streams/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-7952: use in memory stores for ktable test (#19218).

**Test**
Test modified in KTableKTableLeftJoinTest.java covering the corrected behaviour.
