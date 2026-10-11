**Commit**: 7d53410d8f · **Ticket**: KAFKA-7952
**Subsystem**: streams/src
**Category**: Incorrect filtering / wrong query result

**Root cause**
Switch KTableAggregateTest to in-memory stores to reduce test runtime.

**Before/After**
```java
// Before
        final StreamsBuilder builder = new StreamsBuilder();
        final StreamsBuilder builder = new StreamsBuilder();
        final StreamsBuilder builder = new StreamsBuilder();
        final StreamsBuilder builder = new StreamsBuilder();
        final StreamsBuilder builder = new StreamsBuilder();
        final StreamsBuilder builder = new StreamsBuilder();
        final StreamsBuilder builder = new StreamsBuilder();

// After
import org.apache.kafka.streams.TopologyConfig;
import org.apache.kafka.streams.state.BuiltInDslStoreSuppliers;
import org.apache.kafka.test.StreamsTestUtils;

    private StreamsBuilder createStreamBuilderInMemory() {
        final Properties props = StreamsTestUtils.getStreamsConfig(Serdes.String(), Serdes.String());
        props.put(StreamsConfig.DSL_STORE_SUPPLIERS_CLASS_CONFIG,
                    BuiltInDslStoreSuppliers.InMemoryDslStoreSuppliers.class.getName());
        return new StreamsBuilder(new TopologyConfig(new StreamsConfig(props)));
    }
        final StreamsBuilder builder = createStreamBuilderInMemory();
        final StreamsBuilder builder = createStreamBuilderInMemory();
```

**How to spot**
When reviewing streams/src code, look for incorrect filtering / wrong query result patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-7952: switch stream store from rocksdb to in-memory (#21648).

**Test**
Test modified in KTableAggregateTest.java covering the corrected behaviour.
