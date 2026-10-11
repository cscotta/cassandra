**Commit**: 501da383fa · **Ticket**: KAFKA-18026
**Subsystem**: streams/src
**Category**: Incorrect filtering / wrong query result

**Root cause**
This patch transitions the KTable#filter implementation to provide the materialized store via the ProcessorSupplier so that it can be wrapped by the processor wrapper if the wrapper is configured

Reviewers: Anna Sophie Blee-Goldman <ableegoldman@apache.org>

**Before/After**
```java
// Before
                 final String queryableName) {
            new KTableFilter<>(this, predicate, filterNot, queryableStoreName);
        final GraphNode tableNode = new TableFilterNode<>(
            name,
            processorParameters,
            storeFactory
        );
import org.apache.kafka.streams.processor.internals.StoreFactory;
                           final ProcessorParameters<K, V, ?, ?> processorParameters,
                           final StoreFactory storeFactory) {
        super(nodeName, processorParameters, storeFactory, null);

// After
import org.apache.kafka.streams.processor.internals.StoreFactory;
import org.apache.kafka.streams.state.StoreBuilder;
import java.util.Collections;
import java.util.Set;

    private final StoreFactory storeFactory;
                 final String queryableName,
                 final StoreFactory storeFactory) {
        this.storeFactory = storeFactory;
    @Override
    public Set<StoreBuilder<?>> stores() {
        if (storeFactory == null) {
```

**How to spot**
When reviewing streams/src code, look for incorrect filtering / wrong query result patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-18026: transition ktable#filter impl to use processor wrapper (#18205).

**Test**
Test modified in StreamsBuilderTest.java covering the corrected behaviour.
