**Commit**: 7c1bb1585f · **Ticket**: KAFKA-16308
**Subsystem**: core/src
**Category**: Logic error in condition

**Root cause**
As per KIP-1022, we will rename the unstable metadata versions enabled config to support all feature versions. Features is also updated to return latest production and latest testing versions of each feature. A feature is production ready when the corresponding metadata version (bootstrapMetadataVersion) is production ready. Adds tests for the feature usage of the unstableFeatureVersionsEnabled config

Reviewers: David Jacot <djacot@confluent.io>, Jun Rao <junrao@gmail.com>

**Before/After**
```java
// Before
  def createDefault(unstableMetadataVersionsEnabled: Boolean): BrokerFeatures = {
    new BrokerFeatures(defaultSupportedFeatures(unstableMetadataVersionsEnabled))
  def defaultSupportedFeatures(unstableMetadataVersionsEnabled: Boolean): Features[SupportedVersionRange] = {
          if (unstableMetadataVersionsEnabled) {
    PRODUCTION_FEATURES.forEach { feature =>
        features.put(feature.featureName, new SupportedVersionRange(0, feature.latestProduction()))
  val brokerFeatures: BrokerFeatures = BrokerFeatures.createDefault(config.unstableMetadataVersionsEnabled)
        QuorumFeatures.defaultFeatureMap(config.unstableMetadataVersionsEnabled),
        QuorumFeatures.defaultFeatureMap(config.unstableMetadataVersionsEnabled),
      // This indicates whether unreleased MetadataVersions should be enabled on this node.
      .defineInternal(ServerConfigs.UNSTABLE_METADATA_VERSIONS_ENABLE_CONFIG, BOOLEAN, false, HIGH)
  val unstableMetadataVersionsEnabled = getBoolean(ServerConfigs.UNSTABLE_METADATA_VERSIONS_ENABLE_CONFIG)

// After
  def createDefault(unstableFeatureVersionsEnabled: Boolean): BrokerFeatures = {
    new BrokerFeatures(defaultSupportedFeatures(unstableFeatureVersionsEnabled))
  def defaultSupportedFeatures(unstableFeatureVersionsEnabled: Boolean): Features[SupportedVersionRange] = {
          if (unstableFeatureVersionsEnabled) {
    PRODUCTION_FEATURES.forEach { feature => features.put(feature.featureName,
          new SupportedVersionRange(0,
            if (unstableFeatureVersionsEnabled) {
              feature.latestTesting
            } else {
              feature.latestProduction
            }))
  val brokerFeatures: BrokerFeatures = BrokerFeatures.createDefault(config.unstableFeatureVersionsEnabled)
```

**How to spot**
When reviewing core/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-16308 [2/n]: allow unstable feature versions and rename unstable metadata config (#16130).

**Test**
Test modified in KafkaClusterTestKit.java covering the corrected behaviour.
