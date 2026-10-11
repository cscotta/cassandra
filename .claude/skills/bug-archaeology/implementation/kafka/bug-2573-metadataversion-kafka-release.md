**Commit**: 132e0970fb · **Ticket**: KAFKA-17018
**Subsystem**: server-common/src
**Category**: Logic error in condition

**Root cause**
- Mark 3.9-IV0 as stable. Metadata version 3.9-IV0 should return Fetch version 17. - Move ELR to 4.0-IV0. Remove 3.9-IV1 since it's no longer needed.

**Before/After**
```java
// Before
    MetadataVersion metadataVersion() default MetadataVersion.IBP_4_0_IV0;
      MetadataVersion.IBP_3_9_IV0,
      MetadataVersion.IBP_3_9_IV1
  @ClusterTest(types = Array(Type.ZK), brokers = 3, metadataVersion = MetadataVersion.IBP_3_9_IV1, serverProperties = Array(
        setBootstrapMetadataVersion(MetadataVersion.IBP_3_9_IV1).
    assertTrue(stream.toString().contains("3.8-IV0"),
                return MetadataVersion.IBP_3_9_IV1;
    @ValueSource(strings = {"3.6-IV0", "3.7-IV2", "3.9-IV1"})
    @ValueSource(strings = {"3.6-IV0", "3.7-IV2", "3.9-IV1"})

// After
    MetadataVersion metadataVersion() default MetadataVersion.IBP_4_0_IV1;
      MetadataVersion.IBP_3_9_IV0
  @ClusterTest(types = Array(Type.ZK), brokers = 3, metadataVersion = MetadataVersion.IBP_3_9_IV0, serverProperties = Array(
        setBootstrapMetadataVersion(MetadataVersion.IBP_3_9_IV0).
    assertTrue(stream.toString().contains("3.9-IV0"),
                return MetadataVersion.IBP_4_0_IV0;
    @ValueSource(strings = {"3.6-IV0", "3.7-IV2", "4.0-IV0"})
    @ValueSource(strings = {"3.6-IV0", "3.7-IV2", "4.0-IV0"})
```

**How to spot**
When reviewing server-common/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-17018: update metadataversion for the kafka release 3.9 (#16841).

**Test**
Test modified in ClusterTest.java covering the corrected behaviour.
