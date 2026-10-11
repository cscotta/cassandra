**Commit**: 84fe66827d · **Ticket**: KAFKA-18006
**Subsystem**: build.gradle
**Category**: Logic error in condition

**Root cause**
This commit adds AK 3.9 to the system tests on trunk. Follow-up of #17797

Reviewers: Chia-Ping Tsai <chia7712@gmail.com>, Bruno Cadonna <cadonna@apache.org>

**Before/After**
```java
// Before
        UPGRADE_FROM_38 + "(for upgrading from the corresponding old version).";
    UPGRADE_FROM_38("3.8");

// After
            ':streams:upgrade-system-tests-39:test',
project(':streams:upgrade-system-tests-39') {
  base {
    archivesName = "kafka-streams-upgrade-system-tests-39"
  }

  dependencies {
    testImplementation libs.kafkaStreams_39
    testRuntimeOnly libs.junitJupiter
  }

  systemTestLibs {
```

**How to spot**
When reviewing build.gradle code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-18006: add 3.9.0 to end-to-end test (streams) (#17800).

**Test**
Test modified in SmokeTestClient.java covering the corrected behaviour.
