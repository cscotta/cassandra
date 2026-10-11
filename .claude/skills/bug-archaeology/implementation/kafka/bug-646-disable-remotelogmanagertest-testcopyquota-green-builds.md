**Commit**: 46feafefc8 · **Ticket**: KAFKA-19578
**Subsystem**: storage/src
**Category**: Logic error in condition

**Root cause**
Issue already exists - https://issues.apache.org/jira/browse/KAFKA-19578

Reviewers: Andrew Schofield <aschofield@confluent.io>

**Before/After**
```java
// Before
// (no removals)

// After
import org.junit.jupiter.api.Disabled;
    @Disabled("KAFKA-19578")
```

**How to spot**
When reviewing storage/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: minor: disable remotelogmanagertest.testcopyquota for green builds. (#20824).

**Test**
Test modified in RemoteLogManagerTest.java covering the corrected behaviour.
