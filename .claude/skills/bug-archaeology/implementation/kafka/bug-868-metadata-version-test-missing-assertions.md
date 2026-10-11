**Commit**: f33c5e9d72 · **Ticket**: KAFKA-19512

**Subsystem**: KRaft / MetadataVersion

**Category**: Missing test coverage

**Root cause**
`MetadataVersionTest` was missing assertions for three recently introduced `MetadataVersion` enum values: `4.1-IV1`, `4.2-IV0`, and `4.2-IV1`. Without these assertions the test suite would not catch regressions such as incorrect ordinal assignments, missing feature flags, or wrong `IBP_` constant mappings when new versions are added. The fix adds the missing `assertEquals`/`assertTrue` checks covering the expected properties of each new version.

**Before/After**
```java
// Before: test ends at the previous highest version, 4.1-IV0
assertEquals(MetadataVersion.IBP_4_1_IV0, MetadataVersion.latest());

// After: assertions added for new versions
assertTrue(MetadataVersion.IBP_4_1_IV1.isAtLeast(MetadataVersion.IBP_4_1_IV0));
assertTrue(MetadataVersion.IBP_4_2_IV0.isAtLeast(MetadataVersion.IBP_4_1_IV1));
assertTrue(MetadataVersion.IBP_4_2_IV1.isAtLeast(MetadataVersion.IBP_4_2_IV0));
```

**How to spot**
After adding a new `MetadataVersion` enum constant, check whether `MetadataVersionTest` has a corresponding assertion block; a missing block means the new version's invariants are untested.

**Test**
`MetadataVersionTest` now explicitly validates the ordering and feature flags of `4.1-IV1`, `4.2-IV0`, and `4.2-IV1`.
