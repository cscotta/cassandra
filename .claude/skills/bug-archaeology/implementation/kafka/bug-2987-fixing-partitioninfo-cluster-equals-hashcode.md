**Commit**: a8e99eb969 · **Ticket**: KAFKA-16833
**Subsystem**: clients/src
**Category**: Logic error in condition

**Root cause**
Reviewers: Andrew Schofield <aschofield@confluent.io>, Chia-Ping Tsai <chia7712@gmail.com>

**Before/After**
```java
// Before
                Objects.equals(clusterResource, cluster.clusterResource);
                partitionsByTopicPartition, clusterResource);

// After
                Objects.equals(clusterResource, cluster.clusterResource) &&
                Objects.equals(topicIds, cluster.topicIds);
                partitionsByTopicPartition, clusterResource, topicIds);
import java.util.Arrays;
import java.util.Objects;

    @Override
    public int hashCode() {
        return Objects.hash(topic, partition, leader, Arrays.hashCode(replicas),
            Arrays.hashCode(inSyncReplicas), Arrays.hashCode(offlineReplicas));
    }

```

**How to spot**
When reviewing clients/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-16833 fixing partitioninfo and cluster equals and hashcode (#16062).

**Test**
Test modified in ClusterTest.java covering the corrected behaviour.
