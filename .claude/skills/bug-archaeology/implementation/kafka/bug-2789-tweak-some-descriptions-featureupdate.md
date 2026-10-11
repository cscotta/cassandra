**Commit**: 5b0e96d785 · **Ticket**: KAFKA-17034
**Subsystem**: clients/src
**Category**: Logic error in condition

**Root cause**
Reviewers: Chia-Ping Tsai <chia7712@gmail.com>

**Before/After**
```java
// Before
                    "The downgradeType flag should be set to SAFE or UNSAFE when the provided maxVersionLevel:%d is < 1.",
        return String.format("FeatureUpdate{maxVersionLevel:%d, downgradeType:%s}", maxVersionLevel, upgradeType);

// After
                    "The upgradeType flag should be set to SAFE_DOWNGRADE or UNSAFE_DOWNGRADE when the provided maxVersionLevel:%d is < 1.",
        return String.format("FeatureUpdate{maxVersionLevel:%d, upgradeType:%s}", maxVersionLevel, upgradeType);
```

**How to spot**
When reviewing clients/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-17034 tweak some descriptions in featureupdate (#16448).

**Test**
No test added.
