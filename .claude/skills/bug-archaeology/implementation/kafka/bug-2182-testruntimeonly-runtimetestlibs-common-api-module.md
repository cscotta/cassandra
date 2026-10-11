**Commit**: d9cbbcf69c · **Ticket**: KAFKA-17816
**Subsystem**: build.gradle
**Category**: Logic error in condition

**Root cause**
Reviewers: Ken Huang <s7133700@gmail.com>, Chia-Ping Tsai <chia7712@gmail.com>

**Before/After**
```java
// Before
    implementation libs.junitJupiter
    testRuntimeOnly libs.junitPlatformLanucher

// After
    implementation libs.junitJupiterApi

    testImplementation libs.junitJupiter

    testRuntimeOnly runtimeTestLibs
```

**How to spot**
When reviewing build.gradle code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-17816 add testruntimeonly runtimetestlibs to test-common-api module (#17523).

**Test**
No test added.
