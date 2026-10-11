**Commit**: e7a75805fe · **Ticket**: KAFKA-17050
**Subsystem**: core/src
**Category**: Logic error in condition

**Root cause**
This patch partially reverts `group.version` in trunk. I kept the `GroupVersion` class but removed it from `Features` so it is not advertised. I also kept all the changes in the test framework. I removed the logic to require `group.version=1` to enable the new consumer rebalance protocol.

**Before/After**
```java
// Before
import org.apache.kafka.server.common.{GroupVersion, MetadataVersion}
    val version = metadataCache.features().finalizedFeatures().getOrDefault(GroupVersion.FEATURE_NAME, 0.toShort)
    config.groupCoordinatorRebalanceProtocols.contains(Group.GroupType.CONSUMER) && version >= GroupVersion.GV_1.featureLevel
import org.apache.kafka.server.common.{ApiMessageAndVersion, Features, MetadataVersion}
    if (isNewGroupCoordinatorEnabled()) {
      metadataRecords.add(new ApiMessageAndVersion(
        new FeatureLevelRecord()
          .setName(Features.GROUP_VERSION.featureName)
          .setFeatureLevel(Features.GROUP_VERSION.latestTesting),
        0.toShort
      ))
    }

// After
import org.apache.kafka.server.common.{MetadataVersion}
    config.groupCoordinatorRebalanceProtocols.contains(Group.GroupType.CONSUMER)
import org.apache.kafka.server.common.{ApiMessageAndVersion, MetadataVersion}
import org.apache.kafka.server.common.MetadataVersion
      assertEquals(1, apiVersionsResponse.data().supportedFeatures().size())
```

**How to spot**
When reviewing core/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-17050: revert `group.version` (#16482).

**Test**
Test modified in QuorumTestHarness.scala covering the corrected behaviour.
