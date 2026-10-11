**Commit**: 3c1f965c60 · **Ticket**: KAFKA-18521
**Subsystem**: clients/src
**Category**: State not cleaned up

**Root cause**
Reviewers: Chia-Ping Tsai <chia7712@gmail.com>

**Before/After**
```java
// Before
            apiVersionsResponse.data().zkMigrationReady(),
    private final boolean zkMigrationEnabled;

        return new NodeApiVersions(apiVersions, Collections.emptyList(), false, Collections.emptyList(), -1);
            Collection<SupportedFeatureKey> nodeSupportedFeatures,
            boolean zkMigrationEnabled
        this(nodeApiVersions, nodeSupportedFeatures, zkMigrationEnabled, Collections.emptyList(), -1);
            boolean zkMigrationEnabled,
        this.zkMigrationEnabled = zkMigrationEnabled;

    public boolean zkMigrationEnabled() {
        return zkMigrationEnabled;

// After
        return new NodeApiVersions(apiVersions, Collections.emptyList(), Collections.emptyList(), -1);
            Collection<SupportedFeatureKey> nodeSupportedFeatures
        this(nodeApiVersions, nodeSupportedFeatures, Collections.emptyList(), -1);
        NodeApiVersions versions = new NodeApiVersions(new ApiVersionCollection(), Collections.emptyList());
```

**How to spot**
When reviewing clients/src code, look for state not cleaned up patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-18521 cleanup nodeapiversions zkmigrationenabled field (#18535).

**Test**
Test modified in ApiVersionsTest.java covering the corrected behaviour.
