**Commit**: 6af233e4fc · **Ticket**: KAFKA-18203
**Subsystem**: README.md
**Category**: Logic error in condition

**Root cause**
Reviewers: Chia-Ping Tsai <chia7712@gmail.com>

**Before/After**
```java
// Before
// (no removals)

// After
*Note Please ensure that JDK17 is used when developing Kafka.*

IntelliJ Language Level awareness:

IntelliJ will automatically check Java syntax and compatibility for each module, even if the Java version is not
explicitly set in the Structure > Project Settings > Modules.

```

**How to spot**
When reviewing README.md code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-18203 add a section for java version in intellij idea in readme (#18134).

**Test**
No test added.
