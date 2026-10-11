**Commit**: c62c3899aa · **Ticket**: KAFKA-12829
**Subsystem**: streams/src
**Category**: Logic error in condition

**Root cause**
Reviewers: Chia-Ping Tsai <chia7712@gmail.com>, Matthias J. Sax <matthias@confluent.io>

**Before/After**
```java
// Before
import org.apache.kafka.streams.processor.internals.ProcessorAdapter;
import org.apache.kafka.streams.processor.internals.ProcessorNode;
    /**
     * Adds a global {@link StateStore} to the topology.
     * The {@link StateStore} sources its data from all partitions of the provided input topic.
     * There will be exactly one instance of this {@link StateStore} per Kafka Streams instance.
     * <p>
     * A {@link SourceNode} with the provided sourceName will be added to consume the data arriving from the partitions
     * of the input topic.
     * <p>
     * The provided {@link org.apache.kafka.streams.processor.ProcessorSupplier} will be used to create an {@link ProcessorNode} that will receive all
     * records forwarded from the {@link SourceNode}.

// After
import org.apache.kafka.streams.processor.api.Processor;
import org.apache.kafka.streams.processor.api.ProcessorContext;
import org.apache.kafka.streams.processor.api.Record;
```

**How to spot**
When reviewing streams/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-12829: remove deprecated streamsbuilder#addglobalstore of old processor api (#17059).

**Test**
Test modified in StreamsUncaughtExceptionHandlerIntegrationTest.java covering the corrected behaviour.
