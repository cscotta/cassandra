**Commit**: 07133cebd2 · **Ticket**: KAFKA-16806
**Subsystem**: build.gradle
**Category**: Logic error in condition

**Root cause**
Reviewers: Greg Harris <gharris1727@gmail.com>, Chia-Ping Tsai <chia7712@gmail.com>

**Before/After**
```java
// Before


// After

    testRuntimeOnly libs.junitPlatformLanucher

    testRuntimeOnly libs.junitPlatformLanucher


    testRuntimeOnly libs.junitPlatformLanucher

    testRuntimeOnly libs.junitPlatformLanucher

    testRuntimeOnly libs.junitPlatformLanucher
    testRuntimeOnly libs.junitPlatformLanucher
```

**How to spot**
When reviewing build.gradle code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-16806 explicitly declare junit dependencies for all test modules (#16301).

**Test**
No test added.
