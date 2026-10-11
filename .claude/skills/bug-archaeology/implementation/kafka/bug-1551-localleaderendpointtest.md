**Commit**: bc807083fb · **Ticket**: KAFKA-18486
**Subsystem**: core/src
**Category**: Logic error in condition

**Root cause**
This patch is a first step towards removing `ReplicaManager#becomeLeaderOrFollower`. It updates the `LocalLeaderEndPointTest` tests. Reviewers: Christo Lolov <lolovc@amazon.com>, Ismael Juma <ismael@juma.me.uk>

**Before/After**
```java
// Before
import org.apache.kafka.common.{Node, TopicPartition, Uuid}
import org.apache.kafka.common.requests.LeaderAndIsrRequest
import org.apache.kafka.storage.internals.checkpoint.LazyOffsetCheckpoints
import java.util.Collections
import scala.collection.{Map, Seq}
  val topicId: Uuid = Uuid.randomUuid()
  val topicPartition = new TopicPartition(topic, 5)
      alterPartitionManager = alterPartitionManager)
    val partition = replicaManager.createPartition(topicPartition)
    partition.createLogIfNotExists(isNew = false, isFutureReplica = false,
      new LazyOffsetCheckpoints(replicaManager.highWatermarkCheckpoints.asJava), None)
    // Make this replica the leader.

// After
import org.apache.kafka.common.{TopicPartition, Uuid}
import org.apache.kafka.common.metadata.{PartitionChangeRecord, PartitionRecord, TopicRecord}
import org.apache.kafka.image.{MetadataDelta, MetadataImage, MetadataProvenance}
import scala.collection.Map
  val topicId = Uuid.randomUuid()
  val partition = 5
  val topicPartition = new TopicPartition(topic, partition)
  var image: MetadataImage = _
      alterPartitionManager = alterPartitionManager
    )

    val delta = new MetadataDelta(MetadataImage.EMPTY)
```

**How to spot**
When reviewing core/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-18486; [1/2] update localleaderendpointtest (#18666).

**Test**
Test modified in LocalLeaderEndPointTest.scala covering the corrected behaviour.
