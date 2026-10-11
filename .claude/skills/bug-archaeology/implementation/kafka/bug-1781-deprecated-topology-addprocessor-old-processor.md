**Commit**: 9aa900f076 · **Ticket**: KAFKA-12829
**Subsystem**: streams/src
**Category**: Logic error in condition

**Root cause**
Reviewers: John Huang <pegasashjy@gmail.com>, Chia-Ping Tsai <chia7712@gmail.com>, Bill Bejeck <bill@confluent.io>, Lucas Brutschy <lbrutschy@confluent.io>

**Before/After**
```java
// Before
import org.apache.kafka.streams.processor.internals.ProcessorAdapter;
    /**
     * Add a new processor node that receives and processes records output by one or more parent source or processor
     * node.
     * Any new record output by this processor will be forwarded to its child processor or sink nodes.
     * The supplier should always generate a new instance each time
     * {@link org.apache.kafka.streams.processor.ProcessorSupplier#get()} gets called. Creating a single
     * {@link org.apache.kafka.streams.processor.Processor} object and returning the same object reference in
     * {@link org.apache.kafka.streams.processor.ProcessorSupplier#get()} would be a violation of the supplier pattern
     * and leads to runtime exceptions.
     * If {@code supplier} provides stores via {@link ConnectedStoreProvider#stores()}, the provided {@link StoreBuilder}s
     * will be added to the topology and connected to this processor automatically.

// After
// (no additions)
```

**How to spot**
When reviewing streams/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-12829: remove deprecated topology#addprocessor of old processor api (#18154).

**Test**
Test modified in KStreamKStreamJoinTest.java covering the corrected behaviour.
