**Commit**: c5e9154672 · **Ticket**: KAFKA-17342
**Subsystem**: build.gradle
**Category**: Logic error in condition

**Root cause**
There is a lot of code in group-coordinator which is not share/consumer/classic group specific. Since we are introducing a share-coordinator as part of KIP-932 (in a new module), it would make sense to get the common coordinator functionality into a separate common coordinator module so that share-coordinator need not depend on group-coordinator. Reviewers: Apoorv Mittal <apoorvmittal10@gmail.com>, David Jacot <djacot@confluent.io>, Andrew Schofield <aschofield@confluent.io>, Jeff Kim <jeff.kim@confluent.io>, Chia-Ping Tsai <chia7712@gmail.com>

**Before/After**
```java
// Before
// (no removals)

// After
    implementation project(':coordinator-common')
    implementation project(':coordinator-common')
    testImplementation project(':coordinator-common').sourceSets.test.output
project(':coordinator-common') {
  base {
    archivesName = "kafka-coordinator-common"
  }

  dependencies {
    implementation project(':clients')
    implementation project(':server-common')
    implementation project(':metadata')
```

**How to spot**
When reviewing build.gradle code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-17342 moved common coordinator code to separate module (#16883).

**Test**
Test modified in CoordinatorRecordTest.java covering the corrected behaviour.
