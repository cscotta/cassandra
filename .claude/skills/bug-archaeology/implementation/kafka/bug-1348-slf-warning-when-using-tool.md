**Commit**: c3a9b0fc84 · **Ticket**: KAFKA-18804
**Subsystem**: build.gradle
**Category**: Logic error in condition

**Root cause**
When running the tools from source code, multiple SLF4J bindings are encountered. This is because both the tool and core modules' dependent libraries contain Log4j2 JARs. To eliminate these warnings, this PR removes the Log4j2 JARs from core's dependent libraries. Reviewers: Ken Huang <s7133700@gmail.com>, TengYao Chi <kitingiao@gmail.com>, Chia-Ping Tsai <chia7712@gmail.com>

**Before/After**
```java
// Before
    from (configurations.releaseOnly)

// After
    from (configurations.releaseOnly)
```

**How to spot**
When reviewing build.gradle code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-18804 remove slf4j warning when using tool script (#18918).

**Test**
No test added.
