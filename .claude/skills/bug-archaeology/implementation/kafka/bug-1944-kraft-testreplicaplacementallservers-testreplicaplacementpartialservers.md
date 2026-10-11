**Commit**: b08b64c2d8 · **Ticket**: KAFKA-18098
**Subsystem**: core/src
**Category**: Logic error in condition

**Root cause**
Reviewers: Yung <yungyung7654321@gmail.com>, Colin P. McCabe <cmccabe@apache.org>, Chia-Ping Tsai <chia7712@gmail.com>

**Before/After**
```java
// Before
import java.util.{Collections, Optional}
import org.apache.kafka.common.requests.MetadataResponse.TopicMetadata
  @ValueSource(strings = Array("zk")) // TODO: add kraft support
    validateLeaderAndReplicas(topicMetadata, 0, 2, Set(2, 3, 0, 1))
    validateLeaderAndReplicas(topicMetadata, 1, 3, Set(3, 2, 0, 1))
    validateLeaderAndReplicas(topicMetadata, 2, 0, Set(0, 3, 1, 2))
    validateLeaderAndReplicas(topicMetadata, 3, 1, Set(1, 0, 2, 3))
    validateLeaderAndReplicas(topicMetadata, 4, 2, Set(2, 3, 0, 1))
    validateLeaderAndReplicas(topicMetadata, 5, 3, Set(3, 0, 1, 2))
    validateLeaderAndReplicas(topicMetadata, 6, 0, Set(0, 1, 2, 3))
  @ValueSource(strings = Array("zk")) // TODO: add kraft support
    validateLeaderAndReplicas(topicMetadata, 0, 1, Set(1, 2))

// After
import java.util.Collections
  @ValueSource(strings = Array("kraft"))

    assertEquals(7, topicMetadata.partitionMetadata.size)
    for (partition <- topicMetadata.partitionMetadata.asScala) {
      val replicas = partition.replicaIds.asScala.toSet
      assertEquals(4, replicas.size, s"Partition ${partition.partition} should have 4 replicas")
      assertTrue(replicas.subsetOf(Set(0, 1, 2, 3)), s"Replicas should only include brokers 0-3")
      assertTrue(partition.leaderId.isPresent, s"Partition ${partition.partition} should have a leader")
      assertTrue(replicas.contains(partition.leaderId.get), "Leader should be one of the replicas")
    }
  @ValueSource(strings = Array("kraft"))
```

**How to spot**
When reviewing core/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-18098 add kraft support to testreplicaplacementallservers and testreplicaplacementpartialservers (#17955).

**Test**
Test modified in AddPartitionsTest.scala covering the corrected behaviour.
