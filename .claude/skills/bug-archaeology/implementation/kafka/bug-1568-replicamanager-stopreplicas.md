**Commit**: 256ccd0c0d · **Ticket**: KAFKA-18487
**Subsystem**: core/src
**Category**: Logic error in condition

**Root cause**
This patch removes `ReplicaManager#stopReplicas`. I have ensured that removed unit tests are covered by other existing tests or are updated to use kraft. Reviewers: Ismael Juma <ismael@juma.me.uk>, Chia-Ping Tsai <chia7712@gmail.com>

**Before/After**
```java
// Before
import kafka.server.ReplicaManager.{AtMinIsrPartitionCountMetricName, FailedIsrUpdatesPerSecMetricName, IsrExpandsPerSecMetricName, IsrShrinksPerSecMetricName, LeaderCountMetricName, OfflineReplicaCountMetricName, PartitionCountMetricName, PartitionsWithLateTransactionsCountMetricName, ProducerIdCountMetricName, ReassigningPartitionsMetricName, StopReplicaPartitionState, UnderMinIsrPartitionCountMetricName, UnderReplicatedPartitionsMetricName, createLogReadResult, isListOffsetsTimestampUnsupported}
import org.apache.kafka.metadata.LeaderAndIsr

  case class StopReplicaPartitionState(leaderEpoch: Int, deletePartition: Boolean)
  def stopReplicas(correlationId: Int,
                   controllerId: Int,
                   controllerEpoch: Int,
                   partitionStates: Map[TopicPartition, StopReplicaPartitionState]
                  ): (mutable.Map[TopicPartition, Errors], Errors) = {
    replicaStateChangeLock synchronized {
      stateChangeLogger.info(s"Handling StopReplica request correlationId $correlationId from controller " +
        s"$controllerId for ${partitionStates.size} partitions")

// After
import kafka.server.ReplicaManager.{AtMinIsrPartitionCountMetricName, FailedIsrUpdatesPerSecMetricName, IsrExpandsPerSecMetricName, IsrShrinksPerSecMetricName, LeaderCountMetricName, OfflineReplicaCountMetricName, PartitionCountMetricName, PartitionsWithLateTransactionsCountMetricName, ProducerIdCountMetricName, ReassigningPartitionsMetricName, UnderMinIsrPartitionCountMetricName, UnderReplicatedPartitionsMetricName, createLogReadResult, isListOffsetsTimestampUnsupported}
```

**How to spot**
When reviewing core/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-18487; remove replicamanager#stopreplicas (#18647).

**Test**
Test modified in ReplicaManagerTest.scala covering the corrected behaviour.
