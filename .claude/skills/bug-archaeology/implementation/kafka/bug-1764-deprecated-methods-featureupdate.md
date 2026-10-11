**Commit**: 4d6535e60d · **Ticket**: KAFKA-18290
**Subsystem**: clients/src
**Category**: Resource leak

**Root cause**
Reviewers: Chia-Ping Tsai <chia7712@gmail.com>

**Before/After**
```java
// Before
     * <li>Downgrade of feature version level is not a regular operation/intent. It is only allowed
     * in the controller if the {@link FeatureUpdate} has the allowDowngrade flag set. Setting this
     * flag conveys user intent to attempt downgrade of a feature max version level. Note that
     * despite the allowDowngrade flag being set, certain downgrades may be rejected by the
     * controller if it is deemed impossible.</li>
     * <li>Deletion of a finalized feature version is not a regular operation/intent. It could be
     * done by setting the allowDowngrade flag to true in the {@link FeatureUpdate}, and, setting
     * the max version level to a value less than 1.</li>
    /**
     * @param maxVersionLevel   the new maximum version level for the finalized feature.
     *                          a value of zero is special and indicates that the update is intended to
     *                          delete the finalized feature, and should be accompanied by setting

// After
     * <li>Downgrading a feature version level is not a common operation and should only be
     * performed when necessary. It is permitted only if the {@link FeatureUpdate} specifies the
     * {@code upgradeType} as either {@link FeatureUpdate.UpgradeType#SAFE_DOWNGRADE} or
     * {@link FeatureUpdate.UpgradeType#UNSAFE_DOWNGRADE}.
     * <ul>
     * <li>{@code SAFE_DOWNGRADE}: Allows downgrades that do not lead to metadata loss.</li>
     * <li>{@code UNSAFE_DOWNGRADE}: Permits downgrades that might result in metadata loss.</li>
     * </ul>
     * Note that even with these settings, certain downgrades may still be rejected by the controller
     * if they are considered unsafe or impossible.</li>
     * <li>Deleting a finalized feature version is also not a common operation. To delete a feature,
     * set the {@code maxVersionLevel} to zero and specify the {@code upgradeType} as either
```

**How to spot**
When reviewing clients/src code, look for resource leak patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-18290 remove deprecated methods of featureupdate (#18246).

**Test**
No test added.
