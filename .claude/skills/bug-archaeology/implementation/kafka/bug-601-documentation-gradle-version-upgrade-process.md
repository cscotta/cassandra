**Commit**: a18384b311 · **Ticket**: KAFKA-19756
**Subsystem**: README.md
**Category**: Logic error in condition

**Root cause**
Currently, there is no documentation describing how to upgrade Gradle in
the Kafka project. The upgrade process involves   updating three
separate configuration files with specific checksums and URLs, which can
be error-prone without clear guidance. - Added a new "Upgrading Gradle version" section to README.md
- Documents the 3-step process for upgrading Gradle:
  - Update Gradle version in gradle/dependencies.gradle
  - Update wrapper JAR checksum in wrapper.gradle
  - Regenerate the `gradle-wrapper.properties` and  the wrapper script

Reviewers: Ken Huang <s7133700@gmail.com>, PoAn Yang
 <payang@apache.org>, Ming-Yen Chung <mingyen066@gmail.com>, Chia-Ping
 Tsai <chia7712@gmail.com>

**Before/After**
```java
// Before
// (no removals)

// After
### Upgrading Gradle version ###

See [gradle/wrapper/README.md](gradle/wrapper/README.md) for instructions on upgrading the Gradle version.

# Gradle Wrapper

This directory contains the Gradle wrapper files for the Apache Kafka project.

## Upgrading Gradle version

When upgrading the Gradle version used by the Kafka project, update the following files in order:

```

**How to spot**
When reviewing README.md code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-19756 add documentation for gradle version upgrade process (#20741).

**Test**
No test added.
