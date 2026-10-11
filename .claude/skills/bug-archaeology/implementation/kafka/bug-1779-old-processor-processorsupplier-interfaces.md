**Commit**: f7ae4e8b71 · **Ticket**: KAFKA-12829
**Subsystem**: streams/src
**Category**: Logic error in condition

**Root cause**
Reviewers: Chia-Ping Tsai <chia7712@gmail.com>, Bill Bejeck <bill@confluent.io>

**Before/After**
```java
// Before
    @SuppressWarnings("deprecation") // Old PAPI compatibility.
    public KTableImpl(final String name,
                      final Serde<K> keySerde,
                      final Serde<V> valueSerde,
                      final Set<String> subTopologySourceNodes,
                      final String queryableStoreName,
                      final org.apache.kafka.streams.processor.ProcessorSupplier<?, ?> processorSupplier,
                      final GraphNode graphNode,
                      final InternalStreamsBuilder builder) {
        super(name, keySerde, valueSerde, subTopologySourceNodes, graphNode, builder);
        this.processorSupplier = processorSupplier;
        this.queryableStoreName = queryableStoreName;

// After
    @SuppressWarnings("resource")
    @SuppressWarnings("resource")
    @SuppressWarnings("resource")
    @SuppressWarnings({"unchecked", "resource"})
    @SuppressWarnings({"unchecked", "deprecation", "resource"})
 * Cancellable interface returned in {@link org.apache.kafka.streams.processor.api.ProcessorContext#schedule(Duration, PunctuationType, Punctuator)}.
```

**How to spot**
When reviewing streams/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-12829: remove old processor and processorsupplier interfaces (#18238).

**Test**
Test modified in ProcessorContextImplTest.java covering the corrected behaviour.
