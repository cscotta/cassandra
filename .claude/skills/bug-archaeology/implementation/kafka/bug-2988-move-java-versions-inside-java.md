**Commit**: 701f8e7ad4 · **Ticket**: KAFKA-16802
**Subsystem**: build.gradle
**Category**: Logic error in condition

**Root cause**
Reviewers: Greg Harris <greg.harris@aiven.io>

**Before/After**
```java
// Before
  // We should only set this if Java version is < 9 (--release is recommended for >= 9), but the Scala plugin for IntelliJ sets
  // `-target` incorrectly if this is unset
  sourceCompatibility = minJavaVersion
  targetCompatibility = minJavaVersion

// After
  java {
    // We should only set this if Java version is < 9 (--release is recommended for >= 9), but the Scala plugin for IntelliJ sets
    // `-target` incorrectly if this is unset
    sourceCompatibility = minJavaVersion
    targetCompatibility = minJavaVersion
  }
```

**How to spot**
When reviewing build.gradle code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-16802: move java versions inside java block to resolve deprecation (#16135).

**Test**
No test added.
