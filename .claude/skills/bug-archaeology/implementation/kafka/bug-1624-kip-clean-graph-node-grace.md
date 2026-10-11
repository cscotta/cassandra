**Commit**: c7d3999d4f · **Ticket**: KAFKA-18026
**Subsystem**: streams/src
**Category**: Incorrect filtering / wrong query result

**Root cause**
Minor followup to #18195 that I split out into a separate PR since that one was getting a bit long. Should be rebased & reviewed after that one is merged. Introduces a new class for windowed graph nodes with a grace period defined to improve (slightly) the type safety

Reviewers: Guozhang Wang <guozhang.wang.us@gmail.com>, Almog Gavra <almog@responsive.dev>

**Before/After**
```java
// Before
            final ProcessorGraphNode<K, ?> aggProcessorNode =
                new ProcessorGraphNode<>(
                    new ProcessorParameters<>(parentProcessor, kStreamAggProcessorName)
            final ProcessorGraphNode<K, ?> aggProcessorNode =
                new ProcessorGraphNode<>(
                    new ProcessorParameters<>(parentProcessor, kStreamAggProcessorName)
            final ProcessorGraphNode<K, ?> aggProcessorNode =
                new ProcessorGraphNode<>(
                    new ProcessorParameters<>(parentProcessor, kStreamAggProcessorName)
import org.apache.kafka.streams.processor.internals.StoreFactory;
    <KR, VR> KTable<KR, VR> build(final NamedInternal functionName,
                                  final StoreFactory storeFactory,

// After
import org.apache.kafka.streams.kstream.internals.graph.GracePeriodGraphNode;
            final GracePeriodGraphNode<K, ?> aggProcessorNode =
                new GracePeriodGraphNode<>(
                    new ProcessorParameters<>(parentProcessor, kStreamAggProcessorName),
                    windows.gracePeriodMs()
            final long gracePeriod = sessionWindows.gracePeriodMs() + sessionWindows.inactivityGap();
            final GracePeriodGraphNode<K, ?> aggProcessorNode =
                new GracePeriodGraphNode<>(
                    new ProcessorParameters<>(parentProcessor, kStreamAggProcessorName),
                    gracePeriod
            final GracePeriodGraphNode<K, ?> aggProcessorNode =
                new GracePeriodGraphNode<>(
```

**How to spot**
When reviewing streams/src code, look for incorrect filtering / wrong query result patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-18026: kip-1112, clean up graph node grace period resolution (#18342).

**Test**
Test modified in SuppressScenarioTest.java covering the corrected behaviour.
