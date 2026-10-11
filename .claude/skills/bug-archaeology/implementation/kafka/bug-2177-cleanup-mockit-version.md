**Commit**: 6a37d5cceb · **Ticket**: KAFKA-17827
**Subsystem**: gradle/dependencies.gradle
**Category**: State not cleaned up

**Root cause**
Reviewers: Chia-Ping Tsai <chia7712@gmail.com>

**Before/After**
```java
// Before
// mockito >= 5.5 is required for Java 21 and mockito 5.x requires at least Java 11
// mockito 4.11 is used with Java 8 and Scala 2.13
String mockitoVersion
if (JavaVersion.current().isCompatibleWith(JavaVersion.VERSION_11))
  mockitoVersion = "5.10.0"
else
  mockitoVersion = "4.11.0"

// mockito 4.x has two mocking strategies: subclass (mockito-core) and inline (mockito-inline).
// mockito 5.x has two mocking strategies: inline (mockito-core) and subclass (mockito-subclass).
// The default strategy (i.e. what `mockito-core` uses) changed to `inline` in 5.x because it works better with newer
// Java versions.

// After
  mockito: "5.14.2",
  mockitoCore: "org.mockito:mockito-core:$versions.mockito",
  mockitoJunitJupiter: "org.mockito:mockito-junit-jupiter:$versions.mockito",
```

**How to spot**
When reviewing gradle/dependencies.gradle code, look for state not cleaned up patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-17827 cleanup the mockit version (#17536).

**Test**
No test added.
