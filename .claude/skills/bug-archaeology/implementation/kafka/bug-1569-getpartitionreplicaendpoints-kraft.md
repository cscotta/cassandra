**Commit**: 31d8e68ed1 · **Ticket**: KAFKA-18583
**Subsystem**: core/src
**Category**: Logic error in condition

**Root cause**
Although `MetadataCache`'s `getPartitionReplicaEndpoints` takes a single topic-partition, the `KRaftMetadataCache` implementation iterates over all partitions of the matching topic. This is not necessary and can cause significant performance degradation when the topic has a relatively high number of partitions. Note that this is not a recent regression - it has been a part of `KRaftMetadataCache` since its creation. Reviewers: Ismael Juma <ismael@juma.me.uk>, David Jacot <djacot@confluent.io>

**Before/After**
```java
// Before
      topic.partitions().values().forEach { partition =>
import org.apache.kafka.common.{DirectoryId, Uuid}

// After
      Option(topic.partitions().get(tp.partition())).foreach { partition =>
import org.apache.kafka.common.{DirectoryId, TopicPartition, Uuid}
import java.util.stream.Collectors
  @ParameterizedTest
  @MethodSource(Array("cacheProvider"))
  def testGetPartitionReplicaEndpoints(cache: MetadataCache): Unit = {
    val securityProtocol = SecurityProtocol.PLAINTEXT
    val listenerName = ListenerName.forSecurityProtocol(securityProtocol)

    // Set up broker data for the metadata cache
    val numBrokers = 10
    val fencedBrokerId = numBrokers / 3
```

**How to spot**
When reviewing core/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-18583; fix getpartitionreplicaendpoints for kraft (#18635).

**Test**
Test modified in MetadataCacheTest.scala covering the corrected behaviour.
