**Commit**: 3ced942ad4 · **Ticket**: KAFKA-18710
**Subsystem**: core/src
**Category**: Logic error in condition

**Root cause**
This PR moves yet another publisher to the metadata module. Fixes [1]. [1] - https://issues.apache.org/jira/browse/KAFKA-18710

Reviewers: Chia-Ping Tsai <chia7712@gmail.com>

**Before/After**
```java
// Before
import org.apache.kafka.metadata.publisher.{AclPublisher, DelegationTokenPublisher, ScramPublisher}
          config,
import kafka.server.metadata.{ClientQuotaMetadataManager, DynamicClientQuotaPublisher, DynamicConfigPublisher, DynamicTopicClusterQuotaPublisher, KRaftMetadataCache, KRaftMetadataCachePublisher}
import org.apache.kafka.metadata.publisher.{AclPublisher, DelegationTokenPublisher, FeaturesPublisher, ScramPublisher}
        config,
import org.apache.kafka.metadata.publisher.{AclPublisher, DelegationTokenPublisher, ScramPublisher}
      dynamicClientQuotaPublisher.onMetadataUpdate(delta, newImage)

// After
import org.apache.kafka.metadata.publisher.{AclPublisher, DelegationTokenPublisher, DynamicClientQuotaPublisher, ScramPublisher}
          config.nodeId,
import kafka.server.metadata.{ClientQuotaMetadataManager, DynamicConfigPublisher, DynamicTopicClusterQuotaPublisher, KRaftMetadataCache, KRaftMetadataCachePublisher}
import org.apache.kafka.metadata.publisher.{AclPublisher, DelegationTokenPublisher, DynamicClientQuotaPublisher, FeaturesPublisher, ScramPublisher}
        config.nodeId,
import org.apache.kafka.metadata.publisher.{AclPublisher, DelegationTokenPublisher, DynamicClientQuotaPublisher, ScramPublisher}
      dynamicClientQuotaPublisher.onMetadataUpdate(delta, newImage, manifest)
import java.util.function.Consumer
```

**How to spot**
When reviewing core/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-18710 move dynamicclientquotapublisher to metadata module (#20642).

**Test**
Test modified in BrokerMetadataPublisherTest.scala covering the corrected behaviour.
