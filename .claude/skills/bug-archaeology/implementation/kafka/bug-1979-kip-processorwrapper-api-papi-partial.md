**Commit**: 87b902d35d · **Ticket**: KAFKA-18026
**Subsystem**: streams/src
**Category**: Logic error in condition

**Root cause**
This PR includes the API for KIP-1112 and a partial implementation, which wraps any processors added through the PAPI and the DSL processors that are written to the topology through the ProcessorParameters#addProcessorTo method. Further PRs will complete the implementation by converting the remaining DSL operators to using the #addProcessorTo method, and future-proof the processor writing mechanism to prevent new DSL operators from being implemented incorrectly/without the wrapper

Reviewers: Almog Gavra <almog@responsive.dev>, Guozhang Wang <guozhang.wang.us@gmail.com>

**Before/After**
```java
// Before
        internalTopologyBuilder.addProcessor(name, supplier, parentNames);
        final Set<StoreBuilder<?>> stores = supplier.stores();

// After
import org.apache.kafka.streams.processor.internals.NoOpProcessorWrapper;
    /** {@code processor.wrapper.class} */
    public static final String PROCESSOR_WRAPPER_CLASS_CONFIG = "processor.wrapper.class";
    public static final String PROCESSOR_WRAPPER_CLASS_DOC = "A processor wrapper class or class name that implements the <code>org.apache.kafka.streams.state.ProcessorWrapper</code> interface. "
        + "Must be passed in to the StreamsBuilder or Topology constructor in order to take effect";

            .define(PROCESSOR_WRAPPER_CLASS_CONFIG,
                    Type.CLASS,
                    NoOpProcessorWrapper.class,
                    Importance.LOW,
                    PROCESSOR_WRAPPER_CLASS_DOC)
        final ProcessorSupplier<KIn, VIn, KOut, VOut> wrapped = internalTopologyBuilder.wrapProcessorSupplier(name, supplier);
```

**How to spot**
When reviewing streams/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-18026: kip-1112, processorwrapper api with papi and partial dsl implementation (#17892).

**Test**
Test modified in StreamsBuilderTest.java covering the corrected behaviour.
