**Commit**: 9b776ffc50 · **Ticket**: KAFKA-18026
**Subsystem**: streams/src
**Category**: Logic error in condition

**Root cause**
Covers wrapping of processors and state stores for StreamToTableSource

Reviewers: Guozhang Wang <guozhang.wang.us@gmail.com>, Anna Sophie Blee-Goldman <ableegoldman@apache.org>

**Before/After**
```java
// Before
            processorParameters,
            materializedInternal
import org.apache.kafka.common.utils.Bytes;
import org.apache.kafka.streams.kstream.internals.KTableSource;
import org.apache.kafka.streams.kstream.internals.KeyValueStoreMaterializer;
import org.apache.kafka.streams.kstream.internals.MaterializedInternal;
import org.apache.kafka.streams.processor.internals.StoreFactory;
import org.apache.kafka.streams.state.KeyValueStore;
    private final MaterializedInternal<K, V, ?> materializedInternal;
                             final ProcessorParameters<K, V, ?, ?> processorParameters,
                             final MaterializedInternal<K, V, ?> materializedInternal) {
        this.materializedInternal = materializedInternal;

// After
            processorParameters
                             final ProcessorParameters<K, V, ?, ?> processorParameters) {
        return "StreamToTableNode{" + super.toString() + "}";
        processorParameters.addProcessorTo(topologyBuilder, parentNodeNames());
            .toTable(Named.as("toTable")) // wrapped 6
        assertThat(counter.numWrappedProcessors(), CoreMatchers.is(8));
            "toTable-repartition-filter", "toStream", "toTable"
```

**How to spot**
When reviewing streams/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular:  kafka-18026: kip-1112 convert streamtotablenode  (#18149).

**Test**
Test modified in StreamsBuilderTest.java covering the corrected behaviour.
