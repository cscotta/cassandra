**Commit**: 7fe009b2e7 · **Ticket**: KAFKA-17881
**Subsystem**: build.gradle
**Category**: Logic error in condition

**Root cause**
Reviewers: Chia-Ping Tsai <chia7712@gmail.com>

**Before/After**
```java
// Before
// (no removals)

// After
      options.compilerArgs += ["--release", String.valueOf(minJavaVersion)]
```

**How to spot**
When reviewing build.gradle code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-17881 apply the minjavaversion to test code (#17610).

**Test**
No test added.
