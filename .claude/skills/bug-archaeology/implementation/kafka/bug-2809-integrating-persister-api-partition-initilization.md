**Commit**: 9b5b434e2a · **Ticket**: KAFKA-16999
**Subsystem**: core/src
**Category**: Logic error in condition

**Root cause**
Integrated Persister Read API while initilazing Share Partition. The share partition checks for any pre-existing data from persister and initializes the state accordingly. Reviewers:  Andrew Schofield <aschofield@confluent.io>, Manikumar Reddy <manikumar.reddy@gmail.com>, Abhinav Dixit <adixit@confluent.io>

**Before/After**
```java
// Before
        // Initialize the partition.
        // TODO: Provide implementation to initialize the share partition.

// After
import org.apache.kafka.server.group.share.PartitionAllData;
import org.apache.kafka.server.group.share.PartitionIdLeaderEpochData;
import org.apache.kafka.server.group.share.ReadShareGroupStateParameters;
import org.apache.kafka.server.group.share.ReadShareGroupStateResult;
        // Initialize the share partition by reading the state from the persister.
        ReadShareGroupStateResult response;
        try {
            response = persister.readState(new ReadShareGroupStateParameters.Builder()
                .setGroupTopicPartitionData(new GroupTopicPartitionData.Builder<PartitionIdLeaderEpochData>()
                    .setGroupId(this.groupId)
                    .setTopicsData(Collections.singletonList(new TopicData<>(topicIdPartition.topicId(),
                        Collections.singletonList(PartitionFactory.newPartitionIdLeaderEpochData(topicIdPartition.partition(), 0)))))
```

**How to spot**
When reviewing core/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-16999: integrating persister api in partition initilization (kip-932) (#16397).

**Test**
Test modified in SharePartitionTest.java covering the corrected behaviour.
