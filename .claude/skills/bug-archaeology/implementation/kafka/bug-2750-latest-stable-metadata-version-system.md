**Commit**: 1bec3811ad · **Ticket**: KAFKA-17083
**Subsystem**: server-common/src
**Category**: Logic error in condition

**Root cause**
LATEST_PRODUCTION version in MetadataVersion.java was updated in
both #16347 and #16400, but it was left unchanged in the system
tests. Reviewers: Josep Prat <josep.prat@aiven.io>

**Before/After**
```java
// Before
LATEST_STABLE_METADATA_VERSION = "3.7"

// After
    // If you change the value above please also update
    // LATEST_STABLE_METADATA_VERSION version in tests/kafkatest/version.py
LATEST_STABLE_METADATA_VERSION = "3.8"
```

**How to spot**
When reviewing server-common/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-17083: update latest_stable_metadata_version in system tests (#16533).

**Test**
Test modified in version.py covering the corrected behaviour.
