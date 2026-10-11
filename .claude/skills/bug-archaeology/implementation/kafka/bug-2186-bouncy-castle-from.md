**Commit**: 267220a7af · **Ticket**: KAFKA-17802
**Subsystem**: gradle/dependencies.gradle
**Category**: Logic error in condition

**Root cause**
Reviewers: Chia-Ping Tsai <chia7712@gmail.com>

**Before/After**
```java
// Before
  bcpkix: "1.75",

// After
  bcpkix: "1.78.1",
```

**How to spot**
When reviewing gradle/dependencies.gradle code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-17802 update bouncy castle from 1.75 to 1.78.1 (#17512).

**Test**
No test added.
