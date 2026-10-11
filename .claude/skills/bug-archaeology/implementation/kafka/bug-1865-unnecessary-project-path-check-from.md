**Commit**: 633cab91c1 · **Ticket**: KAFKA-18184
**Subsystem**: build.gradle
**Category**: Logic error in condition

**Root cause**
Reviewers: Chia-Ping Tsai <chia7712@gmail.com>

**Before/After**
```java
// Before
      if (!project.path.startsWith(":connect") && !project.path.startsWith(":storage"))
        options.compilerArgs << "-Xlint:-rawtypes"

// After
      options.compilerArgs << "-Xlint:-rawtypes"
```

**How to spot**
When reviewing build.gradle code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-18184 remove the unnecessary project path check from build.gradle (#18102).

**Test**
No test added.
