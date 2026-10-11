**Commit**: 5a1fb1588d · **Ticket**: KAFKA-18373
**Subsystem**: core/src
**Category**: Logic error in condition

**Root cause**
Reviewers: Mickael Maison <mickael.maison@gmail.com>, Ismael Juma <ismael@juma.me.uk>

**Before/After**
```java
// Before
import kafka.server.metadata.{KRaftMetadataCache, ZkMetadataCache}
      // In ZK mode, we just ensure the broker is alive. Although we do not check for shutting down brokers here,
      // the controller will block them from being added to ISR.
      case zkMetadataCache: ZkMetadataCache =>
        zkMetadataCache.hasAliveBroker(followerReplicaId)

import kafka.server.metadata.{KRaftMetadataCache, ZkMetadataCache}
import org.apache.kafka.server.BrokerFeatures
  def zkMetadataCache(brokerId: Int,
                      metadataVersion: MetadataVersion,
                      brokerFeatures: BrokerFeatures = BrokerFeatures.createEmpty())
  : ZkMetadataCache = {

// After
import kafka.server.metadata.KRaftMetadataCache
import kafka.server.metadata.KRaftMetadataCache
import org.apache.kafka.server.common.FinalizedFeatures
```

**How to spot**
When reviewing core/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-18373: remove zkmetadatacache (#18553).

**Test**
Test modified in ReplicaTest.scala covering the corrected behaviour.
