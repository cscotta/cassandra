**Commit**: ede6c90dfb · **Ticket**: KAFKA-19664
**Subsystem**: .github/workflows
**Category**: Logic error in condition

**Root cause**
This patch updates the Apache Kafka project's build, test, and
dependency configurations. - Java Version Update: The build and test processes have been upgraded
from Java 24 to Java 25. - Scala Version Update: The Scala library version has been updated from
2.13.16 to 2.13.17. -  Dependency Upgrades: Several dependencies have been updated to newer
versions, including mockito (5.14.2 to 5.20.0), zinc (1.10.8 to 1.11.0),
and scala-library/reflect (2.13.16 to 2.13.17).

**Before/After**
```java
// Before
          java-version: 24
        java: [ 24, 17 ]
  # This job downloads all the JUnit XML files and thread dumps from the JDK 24 test runs.
          pattern: junit-thread-dumps-24-*
          pattern: junit-xml-24-*  # Only look at JDK 24 tests for the test catalog
        # Make sure these match build.yml
        java: [ 24, 17 ]
- scala-library-2.13.16
- scala-reflect-2.13.16
We build and test Apache Kafka with 17 and 24. The `release` parameter in javac is set to `11` for the clients
  SCALA_VERSION=2.13.16
  set SCALA_VERSION=2.13.16

// After
          java-version: 17
        java: [ 25, 17 ]
  # This job downloads all the JUnit XML files and thread dumps from the JDK 25 test runs.
          pattern: junit-thread-dumps-25-*
          pattern: junit-xml-25-*  # Only look at JDK 25 tests for the test catalog
        # Make sure these match build.yml and also keep in mind that GitHub Actions build will always use this file from the trunk branch.
        java: [ 25, 17 ]
- scala-library-2.13.17
- scala-reflect-2.13.17
We build and test Apache Kafka with 17 and 25. The `release` parameter in javac is set to `11` for the clients
  SCALA_VERSION=2.13.17
  set SCALA_VERSION=2.13.17
```

**How to spot**
When reviewing .github/workflows code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-19664 support building with java 25 (lts release) (#20561).

**Test**
Test modified in TestSslUtils.java covering the corrected behaviour.
