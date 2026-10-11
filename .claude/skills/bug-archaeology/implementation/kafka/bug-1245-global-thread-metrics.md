**Commit**: 87a299a081 · **Ticket**: KAFKA-18826
**Subsystem**: streams/src
**Category**: Logic error in condition

**Root cause**
When implementing
[KIP-1091](https://cwiki.apache.org/confluence/display/KAFKA/KIP-1091%3A+Improved+Kafka+Streams+operator+metrics)
metrics for the Global Stream thread was overlooked. This ticket adds
the Global Thread metrics so they are available via the KIP-1076 process
of adding external Kafka metrics. The existing integration test has been updated to confirm GlobalThread
metrics are sent via the broker plugin. Reviewers: Matthias Sax <mjsax@apache.org>

**Before/After**
```java
// Before
// (no removals)

// After
import org.apache.kafka.streams.processor.api.Processor;
import org.apache.kafka.streams.processor.api.ProcessorContext;
import org.apache.kafka.streams.processor.api.Record;
import org.apache.kafka.streams.state.KeyValueIterator;
import org.apache.kafka.streams.state.KeyValueStore;
import org.apache.kafka.streams.state.Stores;
    private String globalStoreTopic;
    private Uuid globalStoreConsumerInstanceId;
    private  KeyValueIterator<String, String> globalStoreIterator;
        globalStoreTopic = appId + "-global-store";
        cluster.createTopic(globalStoreTopic, 2, 1);
        if (globalStoreIterator != null) {
```

**How to spot**
When reviewing streams/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-18826: add global thread metrics  (#18953).

**Test**
Test modified in KafkaStreamsTelemetryIntegrationTest.java covering the corrected behaviour.
