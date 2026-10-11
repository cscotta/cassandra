**Commit**: 5243fb9a7d · **Ticket**: KAFKA-18026
**Subsystem**: streams/src
**Category**: Logic error in condition

**Root cause**
This PR is part of the implementation for KIP-1112 (KAFKA-18026). In order to have DSL operators be properly wrapped by the interface suggestion in 1112, we need to make sure they all use the ConnectedStoreProvider#stores method to connect stores instead of manually calling addStateStore. This is a refactor only, there is no new behaviors. Reviewers: Anna Sophie Blee-Goldman <ableegoldman@apache.org>

**Before/After**
```java
// Before
                internalStreamsBuilder, topic + "-");
        internalStreamsBuilder.addStateStore(new StoreBuilderWrapper(builder));
            new StoreBuilderWrapper(storeBuilder),
import org.apache.kafka.streams.processor.internals.StoreBuilderWrapper;
            new StoreBuilderWrapper(storeBuilder),
            stateUpdateSupplier,
            new StoreBuilderWrapper(storeBuilder),
            stateUpdateSupplier,
        final KTableSource<K, V> tableSource = new KTableSource<>(materialized.storeName(), materialized.queryableStoreName());
            .wi

// After
                internalStreamsBuilder,
                topic + "-",
                true /* force materializing global tables */);
        internalStreamsBuilder.addStateStore(StoreBuilderWrapper.wrapStoreBuilder(builder));
            StoreBuilderWrapper.wrapStoreBuilder(storeBuilder),
import org.apache.kafka.streams.processor.internals.StoreDelegatingProcessorSupplier;
            new StoreDelegatingProcessorSupplier<>(stateUpdateSupplier, Set.of(storeBuilder)),
            new StoreDelegatingProcessorSupplier<>(stateUpdateSupplier, Set.of(storeBuilder)),
        final KTableSource<K, V> tableSource = new KTableSource<>(materialized);
```

**How to spot**
When reviewing streams/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-18026: migrate ktablesource to use processersupplier#stores (#17903).

**Test**
Test modified in StreamsBuilderTest.java covering the corrected behaviour.
