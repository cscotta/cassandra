**Commit**: 0e4eebe9c0 · **Ticket**: KAFKA-12895
**Subsystem**: LICENSE-binary
**Category**: Logic error in condition

**Root cause**
Reviewers: Chia-Ping Tsai <chia7712@gmail.com>

**Before/After**
```java
// Before
jackson-module-scala_2.12-2.16.2
scala-collection-compat_2.12-2.10.0
scala-collection-compat_2.13-2.10.0
scala-library-2.12.19
scala-logging_2.12-3.9.5
scala-reflect-2.12.19
scala-java8-compat_2.12-1.0.2
Scala 2.12 and 2.13 are supported and 2.13 is used by default. Scala 2.12 support has been deprecated since
Apache Kafka 3.0 and will be removed in Apache Kafka 4.0 (see [KIP-751](https://cwiki.apache.org/confluence/pages/viewpage.action?pageId=181308218)
for more details). See below for how to use a specific Scala version or all of the supported Scala versions.
### Running a task with one of the Scala versions available (2.12.x or 2.13.x) ###
*Note that if building the jars with a version other than 2.13.x, you need to set the `SCALA_VERSION` variable or change it in `bin/kafka-run-class.sh` to run the quick start.*

// After
Scala 2.13 is the only supported version in Apache Kafka.
    scalaCompileOptions.additionalParameters += ["-opt-warnings", "-Xlint:strict-unsealed-patmat"]
    // Scala 2.13.2 introduces compiler warnings suppression, which is a pre-requisite for -Xfatal-warnings
    scalaCompileOptions.additionalParameters += ["-Xfatal-warnings"]
```

**How to spot**
When reviewing LICENSE-binary code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-12895 drop support for scala 2.12 in kafka 4.0 (#17313).

**Test**
Test modified in ConfigCommandTest.java covering the corrected behaviour.
