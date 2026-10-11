**Commit**: faef80a2e2 · **Ticket**: KAFKA-18026
**Subsystem**: streams/src
**Category**: Incorrect filtering / wrong query result

**Root cause**
Final cleanup of StatefulProcessorNode after converting all stateful operators to adding state stores via implementing the #stores method.

**Before/After**
```java
// Before
import org.apache.kafka.streams.kstream.internals.graph.StatefulProcessorNode;
import org.apache.kafka.streams.processor.api.ProcessorSupplier;
        boolean stateCreated = false;
            final StatefulProcessorNode<K, ?> statefulProcessorNode = getStatefulProcessorNode(
                named.suffixWithOrElseGet(
                    "-cogroup-agg-" + counter++,
                    builder,
                    CogroupedKStreamImpl.AGGREGATE_NAME),
                stateCreated,
                storeFactory,
                parentProcessor);
            statefulProcessorNode.setOutputVersioned(isOutputVersioned);

// After


            final String kStreamAggProcessorName = named.suffixWithOrElseGet(
                "-cogroup-agg-" + counter++,
                builder,
                CogroupedKStreamImpl.AGGREGATE_NAME);
            final ProcessorGraphNode<K, ?> aggProcessorNode =
                new ProcessorGraphNode<>(
                    kStreamAggProcessorName,
                    new ProcessorParameters<>(parentProcessor, kStreamAggProcessorName)
                );
            aggProcessorNode.setOutputVersioned(isOutputVersioned);
```

**How to spot**
When reviewing streams/src code, look for incorrect filtering / wrong query result patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-18026: kip-1112, clean up statefulprocessornode (#18195).

**Test**
Test modified in GraphGraceSearchUtilTest.java covering the corrected behaviour.
