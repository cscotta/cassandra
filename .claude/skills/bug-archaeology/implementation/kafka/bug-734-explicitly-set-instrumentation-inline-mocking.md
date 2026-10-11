**Commit**: 527467d053 · **Ticket**: KAFKA-18356
**Subsystem**: build.gradle
**Category**: Logic error in condition

**Root cause**
Reviewers: Mickael Maison <mickael.maison@gmail.com>, Ismael Juma <ismael@juma.me.uk>

**Before/After**
```java
// Before




// After

  // Workaround for Mockito Java Agent restrictions in Java 21+
  // Starting with Java 21, the JDK restricts libraries from attaching a Java agent
  // to their own JVM. As a result, Mockito’s inline mock maker (mockito-core)
  // fails without explicit instrumentation, and the JVM consistently emits warnings.
  // See also: https://javadoc.io/doc/org.mockito/mockito-core/latest/org.mockito/org/mockito/Mockito.html#mockito-instrumentation
  afterEvaluate { subproject ->
    def hasMockitoCore = subproject.configurations.findAll {
      it.canBeResolved
    }.any { config ->
      config.incoming.dependencies.any { dependency ->
          "$dependency" == libs.mockitoCore
```

**How to spot**
When reviewing build.gradle code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-18356: explicitly set up instrumentation for inline mocking (java 21+) (#18339).

**Test**
No test added.
