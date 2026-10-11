**Commit**: 8bd3746e0c · **Ticket**: KAFKA-17705
**Subsystem**: clients/src
**Category**: Logic error in condition

**Root cause**
Added transaction version 2 to some of the system tests. Also marking TV2 as production ready. Also fixes the defaultVersion test. Reviewers: Jun Rao <jun@confluent.io>

**Before/After**
```java
// Before
        QuorumFeatures.defaultFeatureMap(config.unstableFeatureVersionsEnabled),
        QuorumFeatures.defaultFeatureMap(config.unstableFeatureVersionsEnabled),
    public static Map<String, VersionRange> defaultFeatureMap(boolean enableUnstable) {
                QuorumFeatures.defaultFeatureMap(true),
                QuorumFeatures.defaultFeatureMap(true),
                QuorumFeatures.defaultFeatureMap(true),
                QuorumFeatures.defaultFeatureMap(true),

// After
        log.debug("Updating isTV2 enabled to {} with FinalizedFeaturesEpoch {}", isTransactionV2Enabled, latestFinalizedFeaturesEpoch);
        QuorumFeatures.defaultSupportedFeatureMap(config.unstableFeatureVersionsEnabled),
        QuorumFeatures.defaultSupportedFeatureMap(config.unstableFeatureVersionsEnabled),
    public static Map<String, VersionRange> defaultSupportedFeatureMap(boolean enableUnstable) {
                QuorumFeatures.defaultSupportedFeatureMap(true),
                QuorumFeatures.defaultSupportedFeatureMap(true),
                QuorumFeatures.defaultSupportedFeatureMap(true),
                QuorumFeatures.defaultSupportedFeatureMap(true),
```

**How to spot**
When reviewing clients/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-17705: add transactions v2 system tests and mark as production ready (#18132).

**Test**
Test modified in ClusterControlManagerTest.java covering the corrected behaviour.
