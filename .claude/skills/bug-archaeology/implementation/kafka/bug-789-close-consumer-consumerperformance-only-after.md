**Commit**: 4271fd8c8b · **Ticket**: KAFKA-19564
**Subsystem**: tools/src
**Category**: Resource leak

**Root cause**
Ensure that metrics are retrieved and displayed (when requested) before
`Consumer.close()` is called. This is important because metrics are
technically supposed to be removed on `Consumer.close()`, which means
retrieving them _after_ `close()` would yield an empty map. Reviewers: Andrew Schofield <aschofield@confluent.io>

**Before/After**
```java
// Before
import org.apache.kafka.common.Metric;
import org.apache.kafka.common.MetricName;
import java.util.Map;
            KafkaConsumer<byte[], byte[]> consumer = new KafkaConsumer<>(options.props());
            long bytesRead = 0L;
            long messagesRead = 0L;
            long lastBytesRead = 0L;
            long lastMessagesRead = 0L;
            long currentTimeMs = System.currentTimeMillis();
            long joinStartMs = currentTimeMs;
            long startMs = currentTimeMs;
            consume(consumer, options, totalMessagesRead, totalBytesRead, joinTimeMs,

// After
import org.apache.kafka.clients.consumer.Consumer;
import java.util.function.Function;
        run(args, KafkaConsumer::new);
    }

    static void run(String[] args, Function<Properties, Consumer<byte[], byte[]>> consumerCreator) {
            try (Consumer<byte[], byte[]> consumer = consumerCreator.apply(options.props())) {
                long bytesRead = 0L;
                long messagesRead = 0L;
                long lastBytesRead = 0L;
                long lastMessagesRead = 0L;
                long currentTimeMs = System.currentTimeMillis();
```

**How to spot**
When reviewing tools/src code, look for resource leak patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-19564: close consumer in consumerperformance only after metrics displayed (#20267).

**Test**
Test modified in ConsumerPerformanceTest.java covering the corrected behaviour.
