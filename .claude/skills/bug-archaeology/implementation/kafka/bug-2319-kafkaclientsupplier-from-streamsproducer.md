**Commit**: ce5491b7bf · **Ticket**: KAFKA-16331
**Subsystem**: checkstyle/suppressions.xml
**Category**: Logic error in condition

**Root cause**
With EOSv1 removed, we don't need to create a producer per task, and thus can simplify the code by removing KafkaClientSupplier from the deeply nested StreamsProducer, to simplify the code. Reviewers: Bill Bejeck <bill@confluent.io>

**Before/After**
```java
// Before
              files="StreamThread.java"/>
import java.util.HashMap;
import static org.apache.kafka.streams.processor.internals.ClientUtils.threadProducerClientId;
    private final Map<TaskId, StreamsProducer> taskProducers;
                      final boolean processingThreadsEnabled
                      ) {
        if (processingMode == EXACTLY_ONCE_ALPHA) {
            threadProducer = null;
            taskProducers = new HashMap<>();
        } else { // non-eos and eos-v2
            log.info("Creating thread producer client");
            final String threadIdPrefix = String.format("stream-thread [%s] ", Thread.currentThread().getName());

// After
              files="(StreamThread|ActiveTaskCreator).java"/>
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerConfig;
import static org.apache.kafka.streams.processor.internals.ClientUtils.producerClientId;
    private final int threadIdx;
    private final UUID processId;
    // TODO remove `taskProducers`
    private final Map<TaskId, StreamsProducer> taskProducers = Collections.emptyMap();
                      final int threadIdx,
                      final boolean processingThreadsEnabled) {
        this.threadIdx = threadIdx;
        this.processId = processId;
```

**How to spot**
When reviewing checkstyle/suppressions.xml code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-16331: remove kafkaclientsupplier from streamsproducer (#17259).

**Test**
Test modified in StreamsConfigTest.java covering the corrected behaviour.
