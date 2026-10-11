**Commit**: 9ba7dd68e6 · **Ticket**: KAFKA-19668
**Subsystem**: streams/src
**Category**: Logic error in condition

**Root cause**
With "merge.repartition.topic" optimization enabled, Kafka Streams tries
to push repartition topics upstream, to be able to merge multiple
repartition topics from different downstream branches together. However, it is not safe to push a repartition topic if the parent node
is value-changing: because of potentially changing data types, the
topology might become invalid, and fail with serde error at runtime. The optimization itself work correctly, however, processValues() is not
correctly declared as value-changing, what can lead to invalid
topologies. Reviewers: Bill Bejeck <bill@confluent.io>, Lucas Brutschy
 <lbrutschy@confluent.io>

**Before/After**
```java
// Before
        internalStreamsBuilder = new InternalStreamsBuilder(internalTopologyBuilder);
        internalStreamsBuilder = new InternalStreamsBuilder(internalTopologyBuilder);
    public InternalStreamsBuilder(final InternalTopologyBuilder internalTopologyBuilder) {

// After
        internalStreamsBuilder = new InternalStreamsBuilder(internalTopologyBuilder, false);
        internalStreamsBuilder = new InternalStreamsBuilder(
            internalTopologyBuilder,
            TopologyConfig.InternalConfig.getBoolean(
                topologyConfigs.originals(),
                TopologyConfig.InternalConfig.ENABLE_PROCESS_PROCESSVALUE_FIX,
                false
            )
        );
import java.util.Map;

    public static class InternalConfig {
```

**How to spot**
When reviewing streams/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-19668: processvalue() must be declared as value-changing operation (#20470).

**Test**
Test modified in InternalStreamsBuilderTest.java covering the corrected behaviour.
