**Commit**: 8aee314a46 · **Ticket**: KAFKA-16667
**Subsystem**: core/src
**Category**: Race condition / missing synchronization

**Root cause**
When becoming the active KRaftMigrationDriver, there is another race condition similar to KAFKA-16171. This time, the race is due to a stale read from ZK. After writing to /controller and /controller_epoch, it is possible that a read on /migration is not linearized with the writes that were just made. In other words, we get a stale read on /migration.

**Before/After**
```java
// Before
                    s"Conditional update on KRaft Migration ZNode failed. Expected zkVersion = $version. The failed " +
                  throw new RuntimeException(s"Check op on KRaft Migration ZNode failed. Expected zkVersion = $version. " +
import org.apache.kafka.metadata.KafkaConfigSchema
import org.junit.jupiter.api.Assertions.{assertTrue, fail}
      Utils.closeQuietly(zookeeper, "EmbeddedZookeeper")

// After
                    s"Conditional update on KRaft Migration ZNode failed. Sent zkVersion = $version. The failed " +
                  throw new RuntimeException(s"Check op on KRaft Migration ZNode failed. Sent zkVersion = $version. " +
import kafka.zk.migration.{ZkAclMigrationClient, ZkConfigMigrationClient, ZkDelegationTokenMigrationClient, ZkTopicMigrationClient}
import org.apache.kafka.metadata.{KafkaConfigSchema, PartitionRegistration}
import org.junit.jupiter.api.Assertions.{assertTrue, assertEquals, fail}
  class CapturingZkTopicMigrationClient(zkClient: KafkaZkClient) extends ZkTopicMigrationClient(zkClient) {
    val createdTopics = mutable.Set[String]()
    override def createTopic(
      topicName: String,
      topicId: Uuid,
      partitions: util.Map[Integer, PartitionRegistration],
      state: ZkMigrationLeadershipState
```

**How to spot**
When reviewing core/src code, look for race condition / missing synchronization patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-16667 avoid stale read in kraftmigrationdriver (#15918).

**Test**
Test modified in ZkMigrationFailoverTest.scala covering the corrected behaviour.
