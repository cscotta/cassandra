**Commit**: e41373cef6 · **Ticket**: KAFKA-18242
**Subsystem**: build.gradle
**Category**: Logic error in condition

**Root cause**
Reviewers: Ken Huang <s7133700@gmail.com>, Chia-Ping Tsai <chia7712@gmail.com>

**Before/After**
```java
// Before
// (no removals)

// After
    // Gradle does not support the `release` configuration when performing joint Java-Scala compilation.
    // For more details, refer to https://github.com/gradle/gradle/issues/13762.
    // As a result, we need to explicitly configure the Scala compiler with this setting.
    options.compilerArgs += ["--release", String.valueOf(releaseVersion)]

```

**How to spot**
When reviewing build.gradle code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-18242 the java code in core module is not configured with suitable release version (#18182).

**Test**
No test added.
