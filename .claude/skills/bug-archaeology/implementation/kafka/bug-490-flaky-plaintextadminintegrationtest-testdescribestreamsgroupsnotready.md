**Commit**: 3d6687b8f4 · **Ticket**: KAFKA-20074
**Subsystem**: core/src
**Category**: Logic error in condition

**Root cause**
### Description
This test was flaky because it assumes the Streams group will reach
`GroupState.NOT_READY`, but depending on timing and environment the
Streams changelog topic could be created successfully. When that
happened, the group progressed to `ASSIGNING/RECONCILING/STABLE`, the
test failed to observe `NOT_READY` within the timeout, and it sometimes
produced Reconciliation failed logs during consumer shutdown due to an
unfinished onTasksAssigned-related event. This change makes the behavior deterministic by allowing
`createStreamsGroup()` to inject a `replication factor` into
`StreamsRebalanceData.TopicInfo`. In
`testDescribeStreamsGroupsNotReady`, we pass an intentionally impossible
...

**Before/After**
```java
// Before
                               streamsGroupId: String): AsyncKafkaConsumer[K, V] = {
          changelogTopics.map(c => (c, new StreamsRebalanceData.TopicInfo(Optional.empty(), Optional.empty(), util.Map.of()))).toMap.asJava,
      streamsGroupId = streamsGroupId

// After
                               streamsGroupId: String,
                               replicationFactor: Optional[Short] = Optional.empty()): AsyncKafkaConsumer[K, V] = {
    val boxed: Optional[java.lang.Short] =
      replicationFactor.map[java.lang.Short](s => java.lang.Short.valueOf(s))
          changelogTopics.map(c => (c, new StreamsRebalanceData.TopicInfo(Optional.empty(), boxed, util.Map.of()))).toMap.asJava,
    val unavailableReplicationFactorInThisCluster = 9999.toShort
      streamsGroupId = streamsGroupId,
      replicationFactor = Optional.of(unavailableReplicationFactorInThisCluster)
```

**How to spot**
When reviewing core/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-20074: fix flaky plaintextadminintegrationtest#testdescribestreamsgroupsnotready (#21317).

**Test**
Test modified in IntegrationTestHarness.scala covering the corrected behaviour.
