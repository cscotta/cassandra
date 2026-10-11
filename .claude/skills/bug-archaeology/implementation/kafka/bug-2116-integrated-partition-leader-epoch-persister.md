**Commit**: ff116df015 · **Ticket**: KAFKA-17002
**Subsystem**: core/src
**Category**: State not cleaned up

**Root cause**
The PR integrates leader epoch for partition while invoking Persister APIs. The write RPC is retried once on leader epoch failure. Reviewers: Abhinav Dixit <adixit@confluent.io>, Andrew Schofield <aschofield@confluent.io>, Jun Rao <junrao@gmail.com>, David Arthur <mumrah@gmail.com>

**Before/After**
```java
// Before
            shareFetchData.future().completeExceptionally(e);
        FAILED

// After
            sharePartitionManager.handleFetchException(shareFetchData.groupId(), topicPartitionData.keySet(), shareFetchData.future(), e);
import kafka.cluster.Partition;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.errors.NotLeaderOrFollowerException;

    static int leaderEpoch(ReplicaManager replicaManager, TopicPartition tp) {
        Partition partition = replicaManager.getPartitionOrException(tp);
        if (!partition.isLeader()) {
            log.debug("The broker is not the leader for topic partition: {}-{}", tp.topic(), tp.partition());
            throw new NotLeaderOrFollowerException();
        }
        return partition.getLeaderEpoch();
```

**How to spot**
When reviewing core/src code, look for state not cleaned up patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-17002: integrated partition leader epoch for persister apis (kip-932) (#16842).

**Test**
Test modified in DelayedShareFetchTest.java covering the corrected behaviour.
