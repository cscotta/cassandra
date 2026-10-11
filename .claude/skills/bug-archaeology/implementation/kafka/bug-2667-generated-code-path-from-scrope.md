**Commit**: ee03c5f98a · **Ticket**: KAFKA-17195
**Subsystem**: build.gradle
**Category**: Logic error in condition

**Root cause**
The generated code path added to test scope of sourceSets will make `Gradle` assume there are tests in module, and then it results in failed test task since the module does not define test engine. Reviewers: Chia-Ping Tsai <chia7712@gmail.com>

**Before/After**
```java
// Before
        srcDirs = ["src/generated/java", "src/test/java"]
        srcDirs = ["src/generated/java", "src/test/java"]

// After
        srcDirs = ["src/test/java"]
        srcDirs = ["src/test/java"]
```

**How to spot**
When reviewing build.gradle code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-17195 remove generated code path from test scrope of sourcesets  (#16688).

**Test**
No test added.
