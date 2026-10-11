**Commit**: b4aa1087a9 · **Ticket**: KAFKA-19903

**Subsystem**: clients/clients-integration-tests/src/test/java/org/apache/kafka/clients/consumer/ShareConsumerTest.java

**Category**: Wrong constant/default value

**Root cause**
The throttled-delivery feature (introduced by KIP-1203) lacked integration test coverage verifying the batch-halving and last-delivery-isolation behaviours as delivery count approached the configured maximum. The `SharePartition` code path that computes `throttleRecordsDeliveryLimit = Math.max(2, ceil(maxDeliveryCount/2))` and isolates to single records on the final delivery attempt was only unit-tested. Adding `testFetchWithThrottledDelivery` and `testFetchWithThrottledDeliveryBatchesWithIncreasedDeliveryLimit` closes that gap with real broker integration.

**Before**
```java
// Before: no integration test for throttled delivery path
```

**After**
```java
// After: integration test exercises full delivery-limit cycle
for (int i = 0; i < 5; i++) {
    int expectedFetchCount = (i < throttleDeliveryLimit) ? 100
        : ((i == 4) ? 1 : 50);
    ConsumerRecords<?,?> records = waitedPoll(shareConsumer, 2500L,
        expectedFetchCount);
    assertEquals(expectedFetchCount, records.count());
    records.forEach(r -> shareConsumer.acknowledge(r, RELEASE));
}
```

**How to spot**
Check whether `ShareConsumerTest` contains tests that release all records repeatedly up to `maxDeliveryCount`; if absent, the throttle-halving logic (which only activates after `ceil(maxDeliveryCount/2)` deliveries) is only covered by unit tests.

**Test**
Run `testFetchWithThrottledDelivery` end-to-end and verify batch sizes halve at the expected delivery count and isolate to one record on the final attempt.
