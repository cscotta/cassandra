**Commit**: 4bcbf9fae7 · **Ticket**: KAFKA-18026
**Subsystem**: streams/src
**Category**: Logic error in condition

**Root cause**
Migrates KTableSuppressProcessorSupplier to use the the ProcessorSupplier#stores() method

Reviewers: Guozhang Wang <guozhang.wang.us@gmail.com>, Anna Sophie Blee-Goldman <ableegoldman@apache.org>

**Before/After**
```java
// Before
import org.apache.kafka.streams.processor.internals.StoreBuilderWrapper;
        final ProcessorSupplier<K, Change<V>, K, Change<V>> suppressionSupplier = new KTableSuppressProcessorSupplier<>(
            suppressedInternal,
            storeName,
            this
        );

            StoreBuilderWrapper.wrapStoreBuilder(storeBuilder)
import org.apache.kafka.streams.processor.internals.StoreFactory;

                             final StoreFactory materializedKTableStoreBuilder) {
        super(nodeName, processorParameters, materializedKTableStoreBuilder);

// After
        final ProcessorSupplier<K, Change<V>, K, Change<V>> suppressionSupplier = new KTableSuppressProcessorSupplier<>(
                suppressedInternal,
                storeBuilder,
                this
        );

            new String[]{storeName}
                             final String[] storeNames) {
        super(nodeName, processorParameters, storeNames);
import org.apache.kafka.streams.state.StoreBuilder;
import java.util.Set;

```

**How to spot**
When reviewing streams/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-18026: kip-1112 migrate ktablesuppressprocessorsupplier (#18150).

**Test**
Test modified in StreamsBuilderTest.java covering the corrected behaviour.
