**Commit**: 13c89e8cd7 · **Ticket**: KAFKA-19965

**Subsystem**: clients/clients-integration-tests/src/test/java/org/apache/kafka/clients/consumer/ShareConsumerTest.java

**Category**: Wrong constant/default value

**Root cause**
The throttled-delivery integration test produced exactly 512 records in one batch but left `MAX_POLL_RECORDS_CONFIG` at its default of 500. Because `SharePartition` sizes its internal fetch batches according to `max.poll.records`, the producer's single 512-record batch would be split across two share-partition batches (500 + 12), causing the throttle-size computation to be based on 500 rather than 512 and producing unexpected per-poll counts. A sibling test had already set `MAX_POLL_RECORDS_CONFIG=512` for the same reason; the failing test simply missed the override.

**Before**
```java
// Before
ShareConsumer<byte[], byte[]> shareConsumer = createShareConsumer(
    "group1",
    Map.of(ConsumerConfig.SHARE_ACKNOWLEDGEMENT_MODE_CONFIG, EXPLICIT))
```

**After**
```java
// After
ShareConsumer<byte[], byte[]> shareConsumer = createShareConsumer(
    "group1",
    Map.of(
        ConsumerConfig.SHARE_ACKNOWLEDGEMENT_MODE_CONFIG, EXPLICIT,
        ConsumerConfig.MAX_POLL_RECORDS_CONFIG, 512
    ))
```

**How to spot**
When a test produces a fixed number of records and then asserts on batch-level throttle behaviour, verify that `max.poll.records` is set to at least that number; a mismatch causes the logical batch to be split and makes throttle calculations unpredictable.

**Test**
Run `testThrottledDeliveryBatchBehavior` 100 times after the fix and confirm zero flaky failures where the expected batch count differs from the actual count.
