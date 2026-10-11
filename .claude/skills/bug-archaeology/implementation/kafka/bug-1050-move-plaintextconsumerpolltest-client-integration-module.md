**Commit**: aacf3a4b3d · **Ticket**: KAFKA-19042
**Subsystem**: clients/clients-integration-tests
**Category**: Logic error in condition

**Root cause**
Use Java to rewrite PlaintextConsumerPollTest by new test infra and move
it to client-integration-tests module. Reviewers: PoAn Yang <payang@apache.org>, TengYao Chi
<kitingiao@gmail.com>, Chia-Ping Tsai <chia7712@gmail.com>

**Before/After**
```java
// Before
    public static <K, V> List<ConsumerRecord<K, V>> consumeRecords(
        Consumer<K, V> consumer,
        List<ConsumerRecord<K, V>> records = new ArrayList<>();
            consumer.poll(Duration.ofMillis(100)).forEach(records::add);
            return records.size() >= numRecords;
        return records;
        var records = consumeRecords(consumer, numRecords);

// After
import static org.junit.jupiter.api.Assertions.assertTrue;
    public static List<ConsumerRecord<byte[], byte[]>> consumeRecords(
        Consumer<byte[], byte[]> consumer,
        return consumeRecords(consumer, numRecords, Integer.MAX_VALUE);
    }

    public static List<ConsumerRecord<byte[], byte[]>> consumeRecords(
        Consumer<byte[], byte[]> consumer,
        int numRecords,
        int maxPollRecords
    ) throws InterruptedException {
        List<ConsumerRecord<byte[], byte[]>> consumedRecords = new ArrayList<>();
```

**How to spot**
When reviewing clients/clients-integration-tests code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-19042 move plaintextconsumerpolltest to client-integration-tests module (#19582).

**Test**
Test modified in ClientsTestUtils.java covering the corrected behaviour.
