**Commit**: ed10fc63a9 · **Ticket**: KAFKA-18026
**Subsystem**: streams/src
**Category**: Logic error in condition

**Root cause**
KAFKA-18026: supply stores for KTable#mapValues using ProcessorSupplier#stores

Reviewers: Guozhang Wang <guozhang.wang.us@gmail.com>, Anna Sophie Blee-Goldman <ableegoldman@apache.org>

**Before/After**
```java
// Before
        final KTableProcessorSupplier<K, V, K, VR> processorSupplier = new KTableMapValues<>(this, mapper, queryableStoreName);
            processorParameters,
            storeFactory
        final StoreFactory resultStore =
            new KeyValueStoreMaterializer<>(materializedInternal);

            ),
            resultStore
                    final String queryableName) {
        super(nodeName, processorParameters, storeFactory);

// After
        final KTableProcessorSupplier<K, V, K, VR> processorSupplier = new KTableMapValues<>(this, mapper, queryableStoreName, storeFactory);
            processorParameters
            )
import org.apache.kafka.streams.processor.internals.StoreFactory;
import org.apache.kafka.streams.state.StoreBuilder;
import java.util.Collections;
import java.util.Set;

    private final StoreFactory storeFactory;
                    final String queryableName,
                    final StoreFactory storeFactory) {
        this.storeFactory = storeFactory;
```

**How to spot**
When reviewing streams/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-18026: supply stores for ktable#mapvalues using processorsupplier#stores (#18155).

**Test**
Test modified in StreamsBuilderTest.java covering the corrected behaviour.
