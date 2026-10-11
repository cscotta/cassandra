**Commit**: 2cfd28ed8b · **Ticket**: KAFKA-19915
**Subsystem**: checkstyle/import-control-storage.xml
**Category**: Logic error in condition

**Root cause**
This PR ports `HostedPartition` from a Scala sealed trait in
`ReplicaManager` to a Java sealed interface in the server module. Reviewers: Mickael Maison <mickael.maison@gmail.com>, Chia-Ping Tsai
 <chia7712@gmail.com>

**Before/After**
```java
// Before
        <allow pkg="org.apache.kafka.server.log" />
        <allow pkg="org.apache.kafka.server.config" />
import kafka.server.HostedPartition.Online
import org.apache.kafka.server.{ActionQueue, DelayedActionQueue, common}
/**
 * Trait to represent the state of hosted partitions. We create a concrete (active) Partition
 * instance when the broker receives a LeaderAndIsr request from the controller or a metadata
 * log record from the Quorum controller indicating that the broker should be either a leader
 * or follower of a partition.
 */
sealed trait HostedPartition


// After
        <allow pkg="org.apache.kafka.server" />
import org.apache.kafka.server.{ActionQueue, DelayedActionQueue, HostedPartition, common}
  protected val allPartitions = new ConcurrentHashMap[TopicPartition, HostedPartition[Partition]]
      case online: HostedPartition.Online[Partition] => topic == online.partition.topic
      case _: HostedPartition.Offline[Partition] => false
      case _: HostedPartition.None[Partition] => false
      case online: HostedPartition.Online[Partition] =>
        online.partition.maybeAddListener(listener)
```

**How to spot**
When reviewing checkstyle/import-control-storage.xml code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-19915: [2/3] move hostedpartition to server module (#21475).

**Test**
Test modified in ReplicaManagerConcurrencyTest.scala covering the corrected behaviour.
