**Commit**: ba61ff0cd9 · **Ticket**: KAFKA-16860
**Subsystem**: core/src
**Category**: State not cleaned up

**Root cause**
This patch introduces the `group.version` feature flag with one version:
1) Version 1 enables the new consumer group rebalance protocol (KIP-848). Reviewers: Justine Olshan <jolshan@confluent.io>

**Before/After**
```java
// Before
import org.apache.kafka.coordinator.group.GroupCoordinator
import org.apache.kafka.server.common.MetadataVersion
    if (!config.isNewGroupCoordinatorEnabled) {
    if (!config.isNewGroupCoordinatorEnabled) {
                  Map<String, String> saslClientProperties, Map<Integer, Map<String, String>> perServerProperties, List<String> tags) {

// After
import org.apache.kafka.coordinator.group.{Group, GroupCoordinator}
import org.apache.kafka.server.common.{GroupVersion, MetadataVersion}
  private def isConsumerGroupProtocolEnabled(): Boolean = {
    val version = metadataCache.features().finalizedFeatures().getOrDefault(GroupVersion.FEATURE_NAME, 0.toShort)
    config.groupCoordinatorRebalanceProtocols.contains(Group.GroupType.CONSUMER) && version >= GroupVersion.GV_1.featureLevel
  }

    if (!isConsumerGroupProtocolEnabled()) {
    if (!isConsumerGroupProtocolEnabled()) {
import org.apache.kafka.server.common.Features;
    private final Map<Features, Short> features;
                  Map<String, String> saslClientProperties, Map<Integer, Map<String, String>> perServerProperties, List<String> tags,
```

**How to spot**
When reviewing core/src code, look for state not cleaned up patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-16860; [1/2] introduce group.version feature flag (#16120).

**Test**
Test modified in ClusterConfig.java covering the corrected behaviour.
