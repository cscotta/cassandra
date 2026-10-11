**Commit**: 2022b4c480 · **Ticket**: KAFKA-16894
**Subsystem**: server-common/src
**Category**: Logic error in condition

**Root cause**
The ShareVersion feature does not make any metadata version changes. As
a result, `SV_1` does not depend on any MV level, and no MV needs to be
defined for the preview of KIP-932. Reviewers: Jun Rao <junrao@gmail.com>, Chia-Ping Tsai
<chia7712@gmail.com>

**Before/After**
```java
// Before
            "Invalid update version 6 for feature metadata.version. Local controller 0 only supports versions 7-29")),
    // Enables share groups. Note, share groups are for preview only in 4.1. (KIP-932).
    IBP_4_1_IV1(27, "4.1", "IV1", false),

    // IBP_4_2_IV0 accordingly. When 4.2 development begins, IBP_4_2_IV0 will cease to be
    IBP_4_2_IV0(28, "4.2", "IV0", false),
    IBP_4_2_IV1(29, "4.2", "IV1", false);
    SV_1(1, MetadataVersion.IBP_4_2_IV0, Map.of(MetadataVersion.FEATURE_NAME, MetadataVersion.IBP_4_1_IV1.featureLevel()));
                "feature had an error: Invalid update version 0 for feature metadata.version. Local controller 3000 only supports versions 7-29", commandOutput);

// After
            "Invalid update version 6 for feature metadata.version. Local controller 0 only supports versions 7-28")),
    // Share groups are preview in 4.1 (KIP-932).
    // Streams groups are early access in 4.1 (KIP-1071).
    // IBP_4_2_IVx accordingly. When 4.2 development begins, IBP_4_2_IV0 will cease to be
    IBP_4_2_IV0(27, "4.2", "IV0", false),
    IBP_4_2_IV1(28, "4.2", "IV1", false);
    SV_1(1, MetadataVersion.IBP_4_2_IV0, Map.of());
                "feature had an error: Invalid update version 0 for feature metadata.version. Local controller 3000 only supports versions 7-28", commandOutput);
```

**How to spot**
When reviewing server-common/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-16894 correct definition of shareversion (#19606).

**Test**
Test modified in FeatureControlManagerTest.java covering the corrected behaviour.
