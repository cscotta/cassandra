**Commit**: 0781b1bad3 · **Ticket**: KAFKA-15474
**Subsystem**: clients/src
**Category**: Logic error in condition

**Root cause**
Reviewers: Chia-Ping Tsai <chia7712@gmail.com>

**Before/After**
```java
// Before
// (no removals)

// After
import org.junit.jupiter.api.Disabled;
    @Disabled("KAFKA-15474")
```

**How to spot**
When reviewing clients/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-15474 disable flaky testwakeupaftersyncgroupreceivedexternalcompletion (#18188).

**Test**
Test modified in AbstractCoordinatorTest.java covering the corrected behaviour.
