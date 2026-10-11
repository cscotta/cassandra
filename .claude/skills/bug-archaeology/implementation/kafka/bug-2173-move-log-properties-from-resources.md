**Commit**: 57abfc2493 · **Ticket**: KAFKA-17838
**Subsystem**: server-common/src
**Category**: Logic error in condition

**Root cause**
Reviewers: Chia-Ping Tsai <chia7712@gmail.com>

**Before/After**
```java
// Before
// (no removals)

// After
// (no additions)
```

**How to spot**
When reviewing server-common/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-17838 move the log4j.properties from resources/test to resources(#17551).

**Test**
Test modified in log4j.properties covering the corrected behaviour.
