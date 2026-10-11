**Commit**: eec9eccacb · **Ticket**: KAFKA-17483
**Subsystem**: core/src
**Category**: Resource leak

**Root cause**
The PR adds capability to complete pending fetch requests on broker shutdown. Reviewers: Andrew Schofield <aschofield@confluent.io>, Manikumar Reddy <manikumar.reddy@gmail.com>

**Before/After**
```java
// Before
        if (!processFetchQueueLock.compareAndSet(false, true)) {

// After
        if (!fetchQueue.isEmpty()) {
            log.warn("Closing SharePartitionManager with pending fetch requests count: {}", fetchQueue.size());
            fetchQueue.forEach(shareFetchPartitionData -> shareFetchPartitionData.future.completeExceptionally(
                Errors.BROKER_NOT_AVAILABLE.exception()));
            fetchQueue.clear();
        }
        if (!acquireProcessFetchQueueLock()) {
    // Visible for testing.
    boolean acquireProcessFetchQueueLock() {
        return processFetchQueueLock.compareAndSet(false, true);
    }

```

**How to spot**
When reviewing core/src code, look for resource leak patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-17483: complete pending share fetch requests on broker close (#17096).

**Test**
Test modified in SharePartitionManagerTest.java covering the corrected behaviour.
