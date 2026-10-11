**Commit**: 70adf746c4 · **Ticket**: KAFKA-18225
**Subsystem**: clients/src
**Category**: Logic error in condition

**Root cause**
This commit ensures that the ClientQuotaCallback#updateClusterMetadata method is executed in KRaft mode. This method is triggered whenever a topic or cluster metadata change occurs. However, in KRaft mode, the current implementation of the updateClusterMetadata API is inefficient due to the requirement of creating a full Cluster object. To address this, a follow-up issue (KAFKA-18239) has been created to explore more efficient mechanisms for providing cluster information to the ClientQuotaCallback without incurring the overhead of a full Cluster object creation.

**Before/After**
```java
// Before
 * Quota callback interface for brokers that enables customization of client quota computation.
     * Metadata update callback that is invoked whenever UpdateMetadata request is received from
     * the controller. This is useful if quota computation takes partitions into account.
          clientQuotaMetadataManager),
import kafka.server.metadata.{AclPublisher, ClientQuotaMetadataManager, DelegationTokenPublisher, DynamicClientQuotaPublisher, DynamicConfigPublisher, KRaftMetadataCache, KRaftMetadataCachePublisher, ScramPublisher}
        clientQuotaMetadataManager))
import org.apache.kafka.common.{Cluster, Node, TopicPartition, Uuid}
import org.apache.kafka.metadata.{ConfigRepository, LeaderAndIsr}

// After
 * Quota callback interface for brokers and controllers that enables customization of client quota computation.
     * This callback is invoked whenever there are changes in the cluster metadata, such as
     * brokers being added or removed, topics being created or deleted, or partition leadership updates.
     * This is useful if quota computation takes partitions into account.
          clientQuotaMetadataManager,
        ),
        new DynamicTopicClusterQuotaPublisher(
          clusterId,
          config,
          sharedServer.metadataPublishingFaultHandler,
          "broker",
          quotaManagers,
```

**How to spot**
When reviewing clients/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-18225 clientquotacallback#updateclustermetadata is unsupported by kraft (#18196).

**Test**
Test modified in CustomQuotaCallbackTest.java covering the corrected behaviour.
