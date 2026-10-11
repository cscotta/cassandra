**Commit**: 06f699664b · **Ticket**: KAFKA-20415
**Subsystem**: server-common/src
**Category**: Logic error in condition

**Root cause**
* DLQ support for share groups KIP-1191 will be gated by a
`share.version` upgrade - slated for 4.4
* In this PR, we have added the appropriate changes and updated tests in
`FeatureCommandTest` and `ApiVersionsRequestTest`

Reviewers: Andrew Schofield <aschofield@confluent.io>

**Before/After**
```java
// Before
      assertEquals(ShareVersion.SV_1.featureLevel(), apiVersionsResponse.data().supportedFeatures().find(ShareVersion.FEATURE_NAME).maxVersion())
    SV_1(1, MetadataVersion.IBP_4_2_IV0, Map.of());
                "share.version", "0", "1", "0",
                "share.version", "0", "1", "0",
                "share.version", "0", "1", "0",
                "share.version", "0", "1", "0",

// After
      assertEquals(ShareVersion.SV_2.featureLevel(), apiVersionsResponse.data().supportedFeatures().find(ShareVersion.FEATURE_NAME).maxVersion())

    // IBP_4_4_IV0 enables dead-letter queue support for share groups (KIP-1191). When this version
    // is finalized, so will the DLQ support.
    SV_1(1, MetadataVersion.IBP_4_2_IV0, Map.of()),

    // Version 2 adds supports for DLQ (KIP-1191)
    SV_2(2, MetadataVersion.IBP_4_4_IV0, Map.of());
    public boolean supportsShareGroupDLQ() {
        return featureLevel >= SV_2.featureLevel;
    }

```

**How to spot**
When reviewing server-common/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-20415: add share.version=2 for gated dlq support. (#21994).

**Test**
Test modified in AbstractApiVersionsRequestTest.scala covering the corrected behaviour.
