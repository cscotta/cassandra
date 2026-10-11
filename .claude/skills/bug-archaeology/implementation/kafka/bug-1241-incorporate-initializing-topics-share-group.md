**Commit**: 37f7434eac · **Ticket**: KAFKA-18827
**Subsystem**: group-coordinator/src
**Category**: Logic error in condition

**Root cause**
* Currently, when we get a heartbeat with new share partition
subscriptions, we return an initialize request to the caller which the
caller executes asynchronously as a timer task. * Meanwhile, if a new heartbeat request comes with same or null
subscription - the same initialize request will be returned since the
`GroupMetadataManager` has no idea about the older in flight request. * In this PR, we have added a new field to the
`ShareGroupStatePartitionMetadata` record `initializingTopics` where
this information can be recorded in the GMM. Consequently, the
subsequent heartbeats can check this field and not return duplicate
initialize requests.

**Before/After**
```java
// Before
            // This ensures that the previous group write has completed successfully
            // before we start the persister initialize phase.
            .thenCompose(
                response -> handlePersisterInitializeResponse(request.groupTopicPartitionData().groupId(), response, defaultResponse)
            ).exceptionally(exception -> {
                log.error("Unable to initialize share group state {}, {}", gtp.groupId(), gtp.topicsData(), exception);
                Errors error = Errors.forException(exception);
                return new ShareGroupHeartbeatResponseData()
                    .setErrorCode(error.code())
                    .setErrorMessage(error.message());
            });

// After
        Map<Uuid, Map.Entry<String, Set<Integer>>> initializingTopics,
        List<ShareGroupStatePartitionMetadataValue.TopicPartitionsInfo> initializingTopicPartitionInfo = initializingTopics.entrySet().stream()
            .map(entry -> new ShareGroupStatePartitionMetadataValue.TopicPartitionsInfo()
                .setTopicId(entry.getKey())
                .setTopicName(entry.getValue().getKey())
                .setPartitions(entry.getValue().getValue().stream().toList()))
            .toList();

                    .setInitializingTopics(initializingTopicPartitionInfo)
                // Adding to timer makes this call async with respect to the heartbeat.
            .handle((response, exp) -> {
                if (exp == null) {
```

**How to spot**
When reviewing group-coordinator/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-18827: incorporate initializing topics in share group heartbeat [4/n] (#19339).

**Test**
Test modified in GroupCoordinatorRecordHelpersTest.java covering the corrected behaviour.
