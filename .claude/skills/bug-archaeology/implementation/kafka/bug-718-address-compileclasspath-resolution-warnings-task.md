**Commit**: bc2f23b879 · **Ticket**: KAFKA-19750
**Subsystem**: build.gradle
**Category**: Logic error in condition

**Root cause**
`tools-api` is already in core module runtime path, so adding it to
`releaseTarGz` causes the resolution conflicts, which will be a fatal
error in gradle 9

Reviewers: Ken Huang <s7133700@gmail.com>, TengYao Chi
 <frankvicky@apache.org>, Chia-Ping Tsai <chia7712@gmail.com>

**Before/After**
```java
// Before
    from(project(':tools:tools-api').jar) { into("libs/") }
    from(project(':tools:tools-api').configurations.runtimeClasspath) { into("libs/") }

// After
    // tools-api is automatically included in releaseTarGz via core's runtimeClasspath.
    // If removed from here, remember to explicitly add it back in the releaseTarGz task.
```

**How to spot**
When reviewing build.gradle code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-19750 address the compileclasspath resolution warnings for the task (#20628).

**Test**
No test added.
