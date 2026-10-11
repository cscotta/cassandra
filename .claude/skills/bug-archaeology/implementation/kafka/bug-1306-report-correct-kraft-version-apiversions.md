**Commit**: d04efca493 · **Ticket**: KAFKA-18979
**Subsystem**: metadata/src
**Category**: Logic error in condition

**Root cause**
Skip kraft.version when applying FeatureLevelRecord records. The kraft.version is stored as control records and not as metadata records. This solution has the benefits of removing from snapshots any FeatureLevelRecord for kraft.version that was incorrectly written to the log and allows ApiVersions to report the correct finalized kraft.version. Reviewers: Colin P.

**Before/After**
```java
// Before
    private static final short MINIMUM_PERSISTED_FEATURE_LEVEL = 4;

// After
import org.apache.kafka.server.common.KRaftVersion;
        } else if (record.name().equals(KRaftVersion.FEATURE_NAME)) {
            // KAFKA-18979 - Skip any feature level record for kraft.version. This has two benefits:
            // 1. It removes from snapshots any FeatureLevelRecord for kraft.version that was incorrectly written to the log
            // 2. Allows ApiVersions to report the correct finalized kraft.version
import org.apache.kafka.server.common.KRaftVersion;
        } else if (record.name().equals(KRaftVersion.FEATURE_NAME)) {
            // KAFKA-18979 - Skip any feature level record for kraft.version. This has two benefits:
            // 1. It removes from snapshots any FeatureLevelRecord for kraft.version that was incorrectly written to the log
            // 2. Allows ApiVersions to report the correct finalized kraft.version
import org.apache.kafka.server.common.KRaftVersion;
    @Test
```

**How to spot**
When reviewing metadata/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-18979; report correct kraft.version in apiversions (#19205).

**Test**
Test modified in FeatureControlManagerTest.java covering the corrected behaviour.
