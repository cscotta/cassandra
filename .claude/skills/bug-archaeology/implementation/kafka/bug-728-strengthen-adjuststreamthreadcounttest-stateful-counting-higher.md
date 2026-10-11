**Commit**: 71c5a426b8 · **Ticket**: KAFKA-12506
**Subsystem**: streams/integration-tests
**Category**: Logic error in condition

**Root cause**
Add count store and output topic; produce 1,000 records across 50 keys
to better exercise concurrency. Reviewers: Matthias J. Sax <matthias@confluent.io>

**Before/After**
```java
// Before
        builder.stream(inputTopic);


// After
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.streams.kstream.KTable;
import org.apache.kafka.streams.kstream.Materialized;
import org.apache.kafka.streams.kstream.Named;
    private static String outputTopic;
        outputTopic = "output" + testId;
        // Build a simple stateful topology to exercise concurrency with state stores
        final KStream<String, String> source = builder.stream(inputTopic);
        final KTable<String, Long> counts = source
            .groupByKey()
```

**How to spot**
When reviewing streams/integration-tests code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-12506: strengthen adjuststreamthreadcounttest with stateful counting and higher throughput (#20540).

**Test**
Test modified in AdjustStreamThreadCountTest.java covering the corrected behaviour.
