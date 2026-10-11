**Commit**: 010b9ff6c6 · **Ticket**: KAFKA-18186
**Subsystem**: build.gradle
**Category**: Wrong serialization / deserialization

**Root cause**
Reviewers: "A. Sophie Blee-Goldman" <ableegoldman@gmail.com>, Chia-Ping Tsai <chia7712@gmail.com>

**Before/After**
```java
// Before
    options.compilerArgs += ["--release", String.valueOf(releaseVersion)]

// After
    options.release = releaseVersion
```

**How to spot**
When reviewing build.gradle code, look for wrong serialization / deserialization patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-18186 set `options.release` to make intellij configure suitable language level automatically (#18104).

**Test**
No test added.
