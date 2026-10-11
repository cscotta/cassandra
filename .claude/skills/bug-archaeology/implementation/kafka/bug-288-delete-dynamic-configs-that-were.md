**Commit**: a35d6492fb · **Ticket**: KAFKA-19851
**Subsystem**: build.gradle
**Category**: Silent skip on non-match

**Root cause**
When upgrading from Kafka 3.x to 4.0, the metadata log may contain
dynamic configurations that were removed in 4.0 (e.g.,
message.format.version per KIP-724). These removed configs cause
InvalidConfigurationException when users attempt to modify any
configuration, because validation checks all existing configs including
the removed ones. Adds filtering to prevent unsupported or invalid configurations from
being applied during metadata replay. The filtering is implemented using
a SupportedConfigChecker interface that is injected via dependency
injection through Builder patterns.

**Before/After**
```java
// Before
        return new KRaftCoordinatorMetadataDelta(new MetadataDelta(metadataImage));
        CoordinatorMetadataDelta delta = new KRaftCoordinatorMetadataDelta(new MetadataDelta(MetadataImage.EMPTY));

// After
    implementation project(':server')
    <allow pkg="org.apache.kafka.server.config" />
        // Note: supportedConfigChecker is not set because CoordinatorMetadataDelta only exposes topic-related methods.
        // No ConfigRecord replay happens through this path, so the checker is never invoked.
        return new KRaftCoordinatorMetadataDelta(
            new MetadataDelta.Builder()
                .setImage(metadataImage)
                .build()
        );
        CoordinatorMetadataDelta delta = new KRaftCoordinatorMetadataDelta(
            new MetadataDelta.Builder()
                .setImage(MetadataImage.EMPTY)
```

**How to spot**
When reviewing build.gradle code, look for silent skip on non-match patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-19851; delete dynamic configs that were removed by kafka  (#21053).

**Test**
Test modified in CoordinatorRuntimeTest.java covering the corrected behaviour.
