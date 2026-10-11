**Commit**: ebbee397a7 · **Ticket**: KAFKA-17990
**Subsystem**: core/src
**Category**: Race condition / missing synchronization

**Root cause**
Several of the ShareConsumerTest integration tests are a bit flaky. This PR tightens up the logic with the aim of eliminating the flakes. Annoyingly the tests seem rock solid locally so this might take some experimentation. Reviewers: Apoorv Mittal <apoorvmittal10@gmail.com>, Divij Vaidya <diviv@amazon.com>, Manikumar Reddy <manikumar.reddy@gmail.com>

**Before/After**
```java
// Before
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
        KafkaShareConsumer<byte[], byte[]> shareConsumer = createShareConsumer(new ByteArrayDeserializer(), new ByteArrayDeserializer(), "group1");
        assertEquals(Collections.emptySet(), shareConsumer.subscription());
        // "Consumer is not subscribed to any topics."
        assertThrows(IllegalStateException.class, () -> shareConsumer.poll(Duration.ofMillis(500)));
        shareConsumer.close();
        KafkaShareConsumer<byte[], byte[]> shareConsumer = createShareConsumer(new ByteArrayDeserializer(), new ByteArrayDeserializer(), "group1");
        Set<String> subscription = Collections.singleton(tp.topic());
        shareConsumer.subscribe(subscription);
        assertEquals(subscription, shareConsumer.subscription());
        ConsumerRecords<byte[], byte[]> records = shareConsumer.poll(Duration.ofMillis(500));
        shareConsumer.close();

// After
import java.util.concurrent.atomic.AtomicBoolean;
        try (KafkaShareConsumer<byte[], byte[]> shareConsumer = createShareConsumer(new ByteArrayDeserializer(), new ByteArrayDeserializer(), "group1")) {
            assertEquals(Collections.emptySet(), shareConsumer.subscription());
            // "Consumer is not subscribed to any topics."
            assertThrows(IllegalStateException.class, () -> shareConsumer.poll(Duration.ofMillis(500)));
        }
        try (KafkaShareConsumer<byte[], byte[]> shareConsumer = createShareConsumer(new ByteArrayDeserializer(), new ByteArrayDeserializer(), "group1")) {
            Set<String> subscription = Collections.singleton(tp.topic());
            shareConsumer.subscribe(subscription);
            assertEquals(subscription, shareConsumer.subscription());
            ConsumerRecords<byte[], byte[]> records = shareConsumer.poll(Duration.ofMillis(500));
            assertEquals(0, records.count());
```

**How to spot**
When reviewing core/src code, look for race condition / missing synchronization patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-17990: flaky test improvements (#17814).

**Test**
Test modified in ShareConsumerTest.java covering the corrected behaviour.
