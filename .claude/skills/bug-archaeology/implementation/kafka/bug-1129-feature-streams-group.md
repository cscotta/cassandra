**Commit**: b0a26bc2f4 · **Ticket**: KAFKA-19173
**Subsystem**: core/src
**Category**: Logic error in condition

**Root cause**
Add new StreamsGroupFeature, disabled by default,  and add "streams" as
default value to `group.coordinator.rebalance.protocols`. Reviewers: Chia-Ping Tsai <chia7712@gmail.com>, David Jacot
<david.jacot@gmail.com>, Lucas Brutschy <lbrutschy@confluent.io>,
Justine Olshan <jolshan@confluent.io>, Andrew Schofield
<aschofield@confluent.io>, Jun Rao <jun@confluent.io>

**Before/After**
```java
// Before
import org.apache.kafka.server.common.{GroupVersion, RequestLocal, TransactionVersion}
      config.groupCoordinatorRebalanceProtocols.contains(Group.GroupType.STREAMS)
    if (protocols.contains(GroupType.STREAMS)) {
      warn(s"Streams groups and the new '${GroupType.STREAMS}' rebalance protocol are enabled. " +
        "This is part of the early access of KIP-1071 and MUST NOT be used in production.")
    }
import org.apache.kafka.server.common.{EligibleLeaderReplicasVersion, GroupVersion, MetadataVersion, ShareVersion, TransactionVersion}
      assertEquals(5, apiVersionsResponse.data().finalizedFeatures().size())
      assertEquals(6, apiVersionsResponse.data().supportedFeatures().size())

// After
import org.apache.kafka.server.common.{GroupVersion, RequestLocal, StreamsVersion, TransactionVersion}
  }
  private def streamsVersion(): StreamsVersion = {
    StreamsVersion.fromFeatureLevel(metadataCache.features.finalizedFeatures.getOrDefault(StreamsVersion.FEATURE_NAME, 0.toShort))
      config.groupCoordinatorRebalanceProtocols.contains(Group.GroupType.STREAMS) &&
      streamsVersion().streamsGroupSupported
import org.apache.kafka.server.common.{EligibleLeaderReplicasVersion, GroupVersion, MetadataVersion, ShareVersion, StreamsVersion, TransactionVersion}
      assertEquals(6, apiVersionsResponse.data().finalizedFeatures().size())
      assertEquals(7, apiVersionsResponse.data().supportedFeatures().size())

      assertEquals(0, apiVersionsResponse.data().supportedFeatures().find(StreamsVersion.FEATURE_NAME).minVersion())
      assertEquals(StreamsVersion.SV_1.featureLevel(), apiVersionsResponse.data().supportedFeatures().find(StreamsVersion.FEATURE_NAME).maxVersion())
```

**How to spot**
When reviewing core/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-19173: add `feature` for "streams" group (#19509).

**Test**
Test modified in AbstractApiVersionsRequestTest.scala covering the corrected behaviour.
