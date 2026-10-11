**Commit**: 9e60fcc87f · **Ticket**: KAFKA-18181
**Subsystem**: core/src
**Category**: Race condition / missing synchronization

**Root cause**
Reviewers: Apoorv Mittal <apoorvmittal10@gmail.com>, Chia-Ping Tsai <chia7712@gmail.com>

**Before/After**
```java
// Before
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import static org.junit.jupiter.api.Assertions.fail;
    public void testMultipleConsumersInGroupConcurrentConsumption(String persister) {
        ExecutorService producerExecutorService = Executors.newFixedThreadPool(producerCount);
        ExecutorService consumerExecutorService = Executors.newFixedThreadPool(consumerCount);

            producerExecutorService.submit(() -> produceMessages(messagesPerProducer));
        ConcurrentLinkedQueue<CompletableFuture<Integer>> futures = new ConcurrentLinkedQueue<>();

// After
import java.util.function.Function;
import java.util.stream.IntStream;
import java.util.stream.Stream;
    public void testMultipleConsumersInGroupConcurrentConsumption(String persister)
            throws InterruptedException, ExecutionException, TimeoutException {
        List<CompletableFuture<Void>> producerFutures = new ArrayList<>();
            producerFutures.add(CompletableFuture.runAsync(() -> produceMessages(messagesPerProducer)));
        List<CompletableFuture<Integer>> consumerFutures = new ArrayList<>();
            consumerFutures.add(CompletableFuture.supplyAsync(() ->
                    consumeMessages(totalMessagesConsumed,
                            producerCount * messagesPerProducer, groupId, consumerNumber,
                            30, true, maxBytes)));
```

**How to spot**
When reviewing core/src code, look for race condition / missing synchronization patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-18181 refactor shareconsumertest (#18105).

**Test**
Test modified in ShareConsumerTest.java covering the corrected behaviour.
