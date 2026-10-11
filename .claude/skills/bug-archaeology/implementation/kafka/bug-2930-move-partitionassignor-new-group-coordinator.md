**Commit**: 7d832cf74f · **Ticket**: KAFKA-14701
**Subsystem**: build.gradle
**Category**: Logic error in condition

**Root cause**
This patch moves the `PartitionAssignor` interface and all the related classes to a newly created `group-coordinator/api` module, following the pattern used by the storage and tools modules. Reviewers: Ritika Reddy <rreddy@confluent.io>, Jeff Kim <jeff.kim@confluent.io>, Chia-Ping Tsai <chia7712@gmail.com>

**Before/After**
```java
// Before
import org.apache.kafka.coordinator.group.assignor.ConsumerGroupPartitionAssignor
package org.apache.kafka.coordinator.group.assignor;
 * The interface is kept in an internal module until KIP-848 is fully
 * implemented and ready to be released.

// After
    implementation project(':group-coordinator:group-coordinator-api')
project(':group-coordinator:group-coordinator-api') {
  base {
    archivesName = "kafka-group-coordinator-api"
  }

  dependencies {
    implementation project(':clients')
  }

  task createVersionFile() {
    def receiptFile = file("$buildDir/kafka/$buildVersionFileName")
```

**How to spot**
When reviewing build.gradle code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-14701; move `partitionassignor` to new `group-coordinator-api` module (#16198).

**Test**
Test modified in AssignmentTestUtil.java covering the corrected behaviour.
