**Commit**: b99be961b8 · **Ticket**: KAFKA-18206
**Subsystem**: connect/runtime
**Category**: Logic error in condition

**Root cause**
related to KAFKA-18206, set features in EmbeddedKafkaCluster in both streams and connect module, note that this PR also fix potential transaction with empty records in sendPrivileged method as transaction version 2 doesn't allow this kind of scenario. Reviewers: Justine Olshan <jolshan@confluent.io>

**Before/After**
```java
// Before
import org.apache.kafka.common.errors.ProducerFencedException;
                ProducerFencedException.class,
          ("max.connections.per.ip", "60"))), exhaustive = true)
            this(BootstrapMetadata.fromVersion(MetadataVersion.latestTesting(), "testkit"));

// After
import org.apache.kafka.common.errors.InvalidProducerEpochException;
                InvalidProducerEpochException.class,
          ("max.connections.per.ip", "60"),
          ("min.insync.replicas", "1"))), exhaustive = true)
import org.apache.kafka.server.common.Feature;
            this(BootstrapMetadata.fromVersions(
                    MetadataVersion.latestTesting(),
                    Feature.PRODUCTION_FEATURES.stream()
                            .collect(Collectors.toMap(
                                    Feature::featureName,
                                    feature -> feature.defaultLevel(MetadataVersion.latestTesting()),
                                    (existing, replacement) -> existing,
```

**How to spot**
When reviewing connect/runtime code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-18206: embeddedkafkacluster must set features (#18189).

**Test**
Test modified in ExactlyOnceSourceIntegrationTest.java covering the corrected behaviour.
