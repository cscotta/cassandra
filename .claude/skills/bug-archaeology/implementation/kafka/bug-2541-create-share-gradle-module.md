**Commit**: 3db6e68c4c · **Ticket**: KAFKA-17346
**Subsystem**: build.gradle
**Category**: Logic error in condition

**Root cause**
Establishes the new `:share` Gradle module. This module is intended to be used for server-side KIP-932 classes that are not part of the new share group coordinator. This patch relocates and renames some existing classes. A small amount of compatibility changes were also made, but do not affect any logic.

**Before/After**
```java
// Before
import kafka.server.FetchSession;
        return FetchSession.partitionsToLogString(partitions, log.isTraceEnabled());

// After
project(':share') {
  base {
    archivesName = "kafka-share"
  }

  dependencies {
    implementation project(':server-common')

    implementation libs.slf4jApi

    testImplementation libs.junitJupiter
    testImplementation libs.slf4jReload4j
```

**How to spot**
When reviewing build.gradle code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-17346: create :share gradle module (#16888).

**Test**
Test modified in SharePartitionManagerTest.java covering the corrected behaviour.
