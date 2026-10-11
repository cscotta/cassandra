**Commit**: d96b68252c · **Ticket**: KAFKA-18399
**Subsystem**: core/src
**Category**: State not cleaned up

**Root cause**
Reviewers: Viktor Somogyi-Vass <viktorsomogyi@gmail.com>, Chia-Ping Tsai <chia7712@gmail.com>

**Before/After**
```java
// Before
import kafka.cluster.{Broker, Partition}
import kafka.controller.KafkaController
import kafka.server.metadata.{ConfigRepository, KRaftMetadataCache, MockConfigRepository, ZkMetadataCache}
import kafka.zk.KafkaZkClient
import org.apache.kafka.server.config.{ConfigType, KRaftConfigs, ReplicationConfigs, ServerConfigs, ServerLogConfigs}
  private val adminManager: ZkAdminManager = mock(classOf[ZkAdminManager])
  private val controller: KafkaController = mock(classOf[KafkaController])
  private val zkClient: KafkaZkClient = mock(classOf[KafkaZkClient])
  // KRaft tests should override this with a KRaftMetadataCache
  private var metadataCache: MetadataCache = MetadataCache.zkMetadataCache(brokerId, MetadataVersion.latestTesting())
  private val brokerEpochManager: ZkBrokerEpochManager = new ZkBrokerEpochManager(metadataCache, controller, None)
                      enableForwarding: Boolean = false,

// After
import kafka.cluster.Partition
import kafka.server.metadata.{ConfigRepository, KRaftMetadataCache, MockConfigRepository}
import org.apache.kafka.server.config.{KRaftConfigs, ReplicationConfigs, ServerConfigs, ServerLogConfigs}
  private var metadataCache: MetadataCache = MetadataCache.kRaftMetadataCache(brokerId, () => KRaftVersion.LATEST_PRODUCTION)

    val properties = TestUtils.createBrokerConfig(brokerId)
    properties.put(KRaftConfigs.NODE_ID_CONFIG, brokerId.toString)
    properties.put(KRaftConfigs.PROCESS_ROLES_CONFIG, "broker")
    val voterId = brokerId + 1
    properties.put(QuorumConfig.QUORUM_VOTERS_CONFIG, s"$voterId@localhost:9093")

```

**How to spot**
When reviewing core/src code, look for state not cleaned up patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-18399 remove zookeeper from kafkaapis (12/n): clean up zkmetadatacache, kafkacontroller and raftsupport (#18542).

**Test**
Test modified in KafkaApisTest.scala covering the corrected behaviour.
