**Commit**: 408d0f614b · **Ticket**: KAFKA-18192
**Subsystem**: build.gradle
**Category**: Logic error in condition

**Root cause**
Reviewers: Chia-Ping Tsai <chia7712@gmail.com>

**Before/After**
```java
// Before
  modulesNeedingJava11 = [":clients", ":streams", ":streams:test-utils", ":streams-scala", ":test-common:test-common-runtime"]

// After
  modulesNeedingJava11 = [":clients", ":generator", ":streams", ":streams:test-utils", ":streams-scala", ":test-common:test-common-runtime"]
```

**How to spot**
When reviewing build.gradle code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-18192 generator module should run under java 11 (#18114).

**Test**
No test added.
