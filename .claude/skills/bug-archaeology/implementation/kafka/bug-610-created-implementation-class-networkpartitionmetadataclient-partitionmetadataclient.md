**Commit**: 8461ae331a · **Ticket**: KAFKA-19800
**Subsystem**: checkstyle/import-control-group-coordinator.xml
**Category**: Logic error in condition

**Root cause**
This PR is part of

[KIP-1226](https://cwiki.apache.org/confluence/display/KAFKA/KIP-1226%3A+Introducing+Share+Partition+Lag+Persistence+and+Retrieval). This PR introduces an implementation class
NetworkPartitionMetadataClient for PartitionMetadataClient, that uses a
NetworkClient to send ListOffsetsRequest to the destination node. The
destination node should be the leader broker for the partitions in the
request and is retrieved from MetadataCache. This new imple class will later be used in GroupCoordinatorService to
find the partition end offsets while computing share partition lag for
DescribeShareGroupOffsets request.

**Before/After**
```java
// Before
            <allow pkg="org.apache.kafka.clients.consumer" />
import org.apache.kafka.coordinator.group.{GroupConfigManager, GroupCoordinator, GroupCoordinatorRecordSerde, GroupCoordinatorService, PartitionMetadataClient}
import org.apache.kafka.metadata.{BrokerState, ListenerInfo, MetadataVersionConfigValidator}
  var partitionMetadataClient: PartitionMetadataClient = _

      partitionMetadataClient = createPartitionMetadataClient()
  private def createPartitionMetadataClient(): PartitionMetadataClient = {
    // This is a no-op implementation of PartitionMetadataClient. It always returns -1 as the latest offset for any
    // requested topic partition.
    // TODO: KAFKA-19800: Implement a real PartitionMetadataClient that can fetch latest offsets via InterBrokerSendThread.
    new PartitionMetadataClient {
      override def listLatestOffsets(topicPartitions: util.Set[TopicPartition]

// After
            <allow pkg="org.apache.kafka.clients" />
import org.apache.kafka.coordinator.group.{GroupConfigManager, GroupCoordinator, GroupCoordinatorRecordSerde, GroupCoordinatorService, NetworkPartitionMetadataClient, PartitionMetadataClient}
import org.apache.kafka.metadata.{BrokerState, ListenerInfo, MetadataCache, MetadataVersionConfigValidator}
  private var partitionMetadataClient: PartitionMetadataClient = _

      partitionMetadataClient = createPartitionMetadataClient(metadataCache)
  private def createPartitionMetadataClient(metadataCache: MetadataCache): PartitionMetadataClient = {
    new NetworkPartitionMetadataClient(
      metadataCache,
      () => NetworkUtils.buildNetworkClient(
        "NetworkPartitionMetadataClient",
        config,
```

**How to spot**
When reviewing checkstyle/import-control-group-coordinator.xml code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-19800-2: created an implementation class networkpartitionmetadataclient for partitionmetadataclient (#20852).

**Test**
Test modified in GroupCoordinatorServiceTest.java covering the corrected behaviour.
