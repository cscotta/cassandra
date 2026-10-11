**Commit**: 8b5d755bf6 · **Ticket**: KAFKA-17685
**Subsystem**: build.gradle
**Category**: Logic error in condition

**Root cause**
Reviewers: David Arthur <mumrah@gmail.com>

**Before/After**
```java
// Before
    testRuntimeOnly libs.slf4jReload4j
    testRuntimeOnly libs.junitPlatformLanucher
    testRuntimeOnly libs.slf4jReload4j
    testRuntimeOnly libs.junitPlatformLanucher
    testRuntimeOnly libs.slf4jReload4j
    testRuntimeOnly libs.junitPlatformLanucher
    testRuntimeOnly libs.slf4jReload4j
    testRuntimeOnly libs.junitPlatformLanucher
    testRuntimeOnly libs.slf4jReload4j
    testRuntimeOnly libs.junitPlatformLanucher
    testRuntimeOnly libs.slf4jReload4j
    testRuntimeOnly libs.junitPlatformLanucher

// After

  runtimeTestLibs = [
    libs.slf4jReload4j,
    libs.junitPlatformLanucher,
  ]
    testRuntimeOnly runtimeTestLibs

    testRuntimeOnly runtimeTestLibs
    testRuntimeOnly runtimeTestLibs
    testRuntimeOnly runtimeTestLibs
    testRuntimeOnly runtimeTestLibs
    testRuntimeOnly runtimeTestLibs
```

**How to spot**
When reviewing build.gradle code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-17685 define common testruntimeonly dependencies (#17355).

**Test**
No test added.
