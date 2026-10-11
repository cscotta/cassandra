**Commit**: 2327e51926 · **Ticket**: KAFKA-20182
**Subsystem**: build.gradle
**Category**: Logic error in condition

**Root cause**
Follow-up to #22012. Removes removeUnusedImports because its engines
(google-java-format, cleanthat) can non-deterministically throw
InvocationTargetException under parallel execution (see
https://github.com/diffplug/spotless/issues/2850 for a similar report). Unused imports are still enforced by checkstyle (UnusedImports +
RedundantImport). Also bumps Spotless from 8.3.0 to 8.4.0.

**Before/After**
```java
// Before
  id 'com.diffplug.spotless' version "8.3.0"
      // The default engine (google-java-format) uses the JDK compiler's internal
      // parser (com.sun.tools.javac.*), which JPMS blocks without --add-exports.
      // Gradle 9.3+ / Spotless 8.x no longer pass those exports to the worker JVM,
      // so the default engine fails at runtime. The cleanthat engine uses the
      // JavaParser library instead, which ships its own Java parser and does not
      // rely on any JDK internal classes, so it works out of the box.
      // See KAFKA-20182.
      removeUnusedImports('cleanthat-javaparser-unnecessaryimport')

// After
  id 'com.diffplug.spotless' version "8.4.0"
      // removeUnusedImports is intentionally omitted because its engines
      // (google-java-format, cleanthat) can non-deterministically throw
      // InvocationTargetException under parallel execution. Unused imports are
      // still enforced by checkstyle (UnusedImports + RedundantImport).
      // Re-enable once https://github.com/diffplug/spotless/issues/2850 is fixed.
```

**How to spot**
When reviewing build.gradle code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-20182 remove removeunusedimports and upgrade spotless to 8.4.0 (#22025).

**Test**
No test added.
