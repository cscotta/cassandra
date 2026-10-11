**Commit**: 1d0c5f2820 · **Ticket**: KAFKA-19667
**Subsystem**: tools/src
**Category**: Resource leak

**Root cause**
Ensure that metrics are retrieved and displayed (when requested) before
ShareConsumer.close() is called. This is important because metrics are
technically supposed to be removed on ShareConsumer.close(), which means
retrieving them after close() would yield an empty map. Related to https://github.com/apache/kafka/pull/20267. Reviewers: Apoorv Mittal <apoorvmittal10@gmail.com>

**Before/After**
```java
// Before
import java.util.concurrent.TimeoutException;
            List<KafkaShareConsumer<byte[], byte[]>> shareConsumers = new ArrayList<>();
                shareConsumers.add(new KafkaShareConsumer<>(options.props()));
                shareConsumer.close(Duration.ofMillis(500));
    private static void consume(List<KafkaShareConsumer<byte[], byte[]>> shareConsumers,
                                long startMs) throws ExecutionException, InterruptedException, TimeoutException {
    private static void consumeMessagesForSingleShareConsumer(KafkaShareConsumer<byte[], byte[]> shareConsumer,

// After
import org.apache.kafka.clients.consumer.ShareConsumer;
import java.util.function.Function;
        run(args, KafkaShareConsumer::new);
    }

    static void run(String[] args, Function<Properties, ShareConsumer<byte[], byte[]>> shareConsumerCreator) {
            List<ShareConsumer<byte[], byte[]>> shareConsumers = new ArrayList<>();
                shareConsumers.add(shareConsumerCreator.apply(options.props()));
            shareConsumers.forEach(shareConsumer -> shareConsumer.close(Duration.ofMillis(500)));
    private static void consume(List<ShareConsumer<byte[], byte[]>> shareConsumers,
                                long startMs) throws ExecutionException, InterruptedException {
    private static void consumeMessagesForSingleShareConsumer(ShareConsumer<byte[], byte[]> shareConsumer,
```

**How to spot**
When reviewing tools/src code, look for resource leak patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-19667: close shareconsumer in shareconsumerperformance after metrics displayed (#20467).

**Test**
Test modified in ShareConsumerPerformanceTest.java covering the corrected behaviour.
