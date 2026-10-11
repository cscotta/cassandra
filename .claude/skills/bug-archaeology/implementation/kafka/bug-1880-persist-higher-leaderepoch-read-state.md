**Commit**: 42f74a1c3a · **Ticket**: KAFKA-17796
**Subsystem**: share-coordinator/src
**Category**: State not cleaned up

**Root cause**
This PR adds code into the ShareCoordinatorService.readState method to issue a runtime.scheduleWriteOperation call if the incoming read state request holds a valid leaderEpoch value (not -1). Co-authored-by: TaiJu Wu <tjwu1217@gmail.com>

Reviewers: Andrew Schofield <aschofield@confluent.io>, David Jacot <djacot@confluent.io>

**Before/After**
```java
// Before
        // Combine all futures into a single CompletableFuture<Void>
                        // map of partition id -> responses from api
            // time taken for write
            // at this point all futures are completed written above.
        // Send an empty response if topic data is empty
        // Send an empty response if partition data is empty for any topic
        // Send an empty response if groupId is invalid
        // Send an empty response if the coordinator is not active
        request.topics().forEach(topicData -> {
            topicData.partitions().forEach(partitionData -> {
                // Request object containing information of a single topic partition

// After
        // Combine all futures into a single CompletableFuture<Void>.
                        // Map of partition id -> responses from api.
            // Time taken for write.
            // At this point all futures are completed written above.
        // Send an empty response if topic data is empty.
        // Send an empty response if partition data is empty for any topic.
        // Send an empty response if groupId is invalid.
        // Send an empty response if the coordinator is not active.
        // It is possible that a read state request contains a leaderEpoch which is the higher than seen so
        // far, for a specific share partition. Hence, for each read request - we must check for this
        // and update the state appropriately.

```

**How to spot**
When reviewing share-coordinator/src code, look for state not cleaned up patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-17796: persist higher leaderepoch in read state call. (#17580).

**Test**
Test modified in ShareCoordinatorServiceTest.java covering the corrected behaviour.
