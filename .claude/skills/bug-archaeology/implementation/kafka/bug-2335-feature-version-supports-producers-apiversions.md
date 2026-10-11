**Commit**: e512b634d4 · **Ticket**: KAFKA-14563
**Subsystem**: clients/src
**Category**: State not cleaned up

**Root cause**
Add the client side feature version handling. Reviewers: Artem Livshits <alivshits@confluent.io>, Justine Olshan <jolshan@confluent.io>

**Before/After**
```java
// Before
            apiVersionsResponse.data().zkMigrationReady());
        return new NodeApiVersions(apiVersions, Collections.emptyList(), false);
    public NodeApiVersions(Collection<ApiVersion> nodeApiVersions, Collection<SupportedFeatureKey> nodeSupportedFeatures, boolean zkMigrationEnabled) {

// After
    // The maximum finalized feature epoch of all the node api versions.
    private long maxFinalizedFeaturesEpoch = -1;
    private Map<String, Short> finalizedFeatures;

    public static class FinalizedFeaturesInfo {
        public final long finalizedFeaturesEpoch;
        public final Map<String, Short> finalizedFeatures;
        FinalizedFeaturesInfo(long finalizedFeaturesEpoch, Map<String, Short> finalizedFeatures) {
            this.finalizedFeaturesEpoch = finalizedFeaturesEpoch;
            this.finalizedFeatures = finalizedFeatures;
        }
    }
```

**How to spot**
When reviewing clients/src code, look for state not cleaned up patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-14563: add feature version supports in the producers and apiversions (#17265).

**Test**
Test modified in ApiVersionsTest.java covering the corrected behaviour.
