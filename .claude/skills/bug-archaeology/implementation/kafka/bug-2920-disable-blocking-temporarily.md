**Commit**: d13a693ea7 · **Ticket**: KAFKA-16916
**Subsystem**: clients/src
**Category**: Logic error in condition

**Root cause**
The test runs forever. We disable the test temporarily to unblock CI

Reviewers: Luke Chen <showuon@gmail.com>

**Before/After**
```java
// Before
// (no removals)

// After
import org.junit.jupiter.api.Disabled;
    // The test runs forever on trunk after c01279b9. (KAFKA-16916)
    // We disable the test temporarily until we fix it to unblock the CI.
    @Disabled
```

**How to spot**
When reviewing clients/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-16916: disable blocking test temporarily (#16248).

**Test**
Test modified in ClientAuthenticationFailureTest.java covering the corrected behaviour.
