**Commit**: e299a006c8 · **Ticket**: KAFKA-17214
**Subsystem**: build.gradle
**Category**: Logic error in condition

**Root cause**
* KAFKA-17214: Add 3.8.0 version to streams system tests

Reviewers: Bill Bejeck <bbejeck@gmail.com>

**Before/After**
```java
// Before
        UPGRADE_FROM_35 + "\", \"" + UPGRADE_FROM_36 + "\", \"" + UPGRADE_FROM_37 + "(for upgrading from the corresponding old version).";
    UPGRADE_FROM_37("3.7");

// After
            ':streams:upgrade-system-tests-38:test',
project(':streams:upgrade-system-tests-38') {
  base {
    archivesName = "kafka-streams-upgrade-system-tests-38"
  }

  dependencies {
    testImplementation libs.kafkaStreams_38
    testRuntimeOnly libs.junitJupiter
  }

  systemTestLibs {
```

**How to spot**
When reviewing build.gradle code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-17214: add 3.8.0 version to streams system tests (#16728).

**Test**
Test modified in SmokeTestClient.java covering the corrected behaviour.
