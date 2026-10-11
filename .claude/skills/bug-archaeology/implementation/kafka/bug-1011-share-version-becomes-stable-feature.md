**Commit**: 223684bad1 · **Ticket**: KAFKA-16894
**Subsystem**: server-common/src
**Category**: Logic error in condition

**Root cause**
This PR sets `SV_1` as the latest-production share version. This means
for AK 4.1 it will be a preview feature, not enabled by default, but can
be enabled without turning on unstable features. This is analogous to
how ELR worked for AK 4.0. Reviewers: Apoorv Mittal <apoorvmittal10@gmail.com>, Chia-Ping Tsai
 <chia7712@gmail.com>

**Before/After**
```java
// Before
    // This is a preview in 4.1, and production-ready in 4.2.
    public static final ShareVersion LATEST_PRODUCTION = SV_0;

// After
    // This is a preview in 4.1, and expected to be production-ready in 4.2.
    public static final ShareVersion LATEST_PRODUCTION = SV_1;
```

**How to spot**
When reviewing server-common/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-16894: share.version becomes stable feature for preview (#19831).

**Test**
No test added.
