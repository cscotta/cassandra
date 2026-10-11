**Commit**: baedfc7e04 · **Ticket**: KAFKA-17160
**Subsystem**: gradle/dependencies.gradle
**Category**: Logic error in condition

**Root cause**
Reviewers: Chia-Ping Tsai <chia7712@gmail.com>

**Before/After**
```java
// Before
if (JavaVersion.current().isCompatibleWith(JavaVersion.VERSION_11))
  mockitoArtifactName = "mockito-core"
else

// After
if (mockitoVersion.startsWith("4."))
else
  mockitoArtifactName = "mockito-core"
```

**How to spot**
When reviewing gradle/dependencies.gradle code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-17160 determine mockitoartifactname based on mockitoversion instead of javaversion (#16639).

**Test**
No test added.
