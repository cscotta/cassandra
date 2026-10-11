**Commit**: 9c2b1b8d0b · **Ticket**: KAFKA-16809
**Subsystem**: build.gradle
**Category**: Logic error in condition

**Root cause**
Signed-off-by: Greg Harris <greg.harris@aiven.io>
Reviewers: Mickael Maison <mickael.maison@gmail.com>

**Before/After**
```java
// Before
// (no removals)

// After
    // Javadoc warnings should fail the build in JDK 15+ https://bugs.openjdk.org/browse/JDK-8200363
    options.addBooleanOption('Werror', JavaVersion.current().isCompatibleWith(JavaVersion.VERSION_15))
  test.dependsOn('javadoc')

```

**How to spot**
When reviewing build.gradle code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-16809: run javadoc in ci (#16025).

**Test**
No test added.
