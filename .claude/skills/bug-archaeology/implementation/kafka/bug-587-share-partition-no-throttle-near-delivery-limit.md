**Commit**: bcd3191c79 · **Ticket**: KAFKA-17541

**Subsystem**: core/src/main/java/kafka/server/share/SharePartition.java

**Category**: Logic error in condition

**Root cause**
When a record's delivery count approached the configured maximum (`maxDeliveryCount`), `SharePartition` continued to return full batches to share consumers on every re-delivery attempt. This meant a broker with a high `maxDeliveryCount` would repeatedly flood consumers with large batches of poison records that could never be successfully processed, consuming network bandwidth and consumer resources. The fix computes a `throttleRecordsDeliveryLimit = Math.max(2, ceil(maxDeliveryCount/2))`: once `deliveryCount >= throttleRecordsDeliveryLimit`, the batch size is halved per additional delivery attempt; on the final attempt only a single record is returned so the consumer can isolate the exact offending message.

**Before**
```java
// Before: full batch returned regardless of delivery count
return acquiredRecords; // batch unchanged near delivery limit
```

**After**
```java
// After: halve batch size after throttleRecordsDeliveryLimit
int throttleLimit = Math.max(2, (int) Math.ceil(maxDeliveryCount / 2.0));
if (deliveryCount >= throttleLimit) {
    int throttledSize = Math.max(1,
        batchSize >> (deliveryCount - throttleLimit + 1));
    return acquiredRecords.subList(0, throttledSize);
}
```

**How to spot**
In `SharePartition.acquire`, look for any code path that returns records to a consumer without consulting the current `deliveryCount` relative to `maxDeliveryCount`; if no batch-size reduction occurs as the count climbs, unprocessable records will be delivered in full batches indefinitely.

**Test**
Run `testFetchWithThrottledDelivery` and assert that batch sizes halve from the third delivery attempt onward and isolate to a single record on the final (fifth) attempt.
