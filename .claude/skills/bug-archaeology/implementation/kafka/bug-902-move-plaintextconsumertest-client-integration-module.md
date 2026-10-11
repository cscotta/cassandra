**Commit**: a399852ced · **Ticket**: KAFKA-19042
**Subsystem**: build.gradle
**Category**: Logic error in condition

**Root cause**
Use Java to rewrite PlaintextConsumerTest by new test infra and  move it
to client-integration-tests module. Reviewers: Jhen-Yung Hsu <jhenyunghsu@gmail.com>, TengYao Chi
<kitingiao@gmail.com>, Chia-Ping Tsai <chia7712@gmail.com>

**Before/After**
```java
// Before
    public static List<ConsumerRecord<byte[], byte[]>> consumeRecords(
        Consumer<byte[], byte[]> consumer,
    public static List<ConsumerRecord<byte[], byte[]>> consumeRecords(
        Consumer<byte[], byte[]> consumer,
        List<ConsumerRecord<byte[], byte[]>> consumedRecords = new ArrayList<>();
    public static void sendAndAwaitAsyncCommit(
        Consumer<byte[], byte[]> consumer,
    public static void sendAsyncCommit(
        Consumer<byte[], byte[]> consumer,
    private static class RetryCommitCallback implements OffsetCommitCallback {
        Consumer<byte[], byte[]> consumer;
            Consumer<byte[], byte[]> consumer,

// After
    testImplementation project(':test-common:test-common-util')
    public static <K, V> List<ConsumerRecord<K, V>> consumeRecords(
        Consumer<K, V> consumer,
    public static <K, V> List<ConsumerRecord<K, V>> consumeRecords(
        Consumer<K, V> consumer,
        List<ConsumerRecord<K, V>> consumedRecords = new ArrayList<>();
    public static void consumeAndVerifyRecordsWithTimeTypeLogAppend(
        Consumer<byte[], byte[]> consumer,
        TopicPartition tp,
        int numRecords,
        long startingTimestamp
    ) throws InterruptedException {
```

**How to spot**
When reviewing build.gradle code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-19042 move plaintextconsumertest to client-integration-tests module  (#20081).

**Test**
Test modified in ClientsTestUtils.java covering the corrected behaviour.
