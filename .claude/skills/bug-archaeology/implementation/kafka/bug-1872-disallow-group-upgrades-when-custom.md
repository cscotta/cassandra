**Commit**: 9ae1b0f017 · **Ticket**: KAFKA-18134
**Subsystem**: group-coordinator/src
**Category**: Logic error in condition

**Root cause**
Disallow upgrades from classic groups to consumer groups when any member's assignment has non-empty userData. Reviewers: David Jacot <djacot@confluent.io>

**Before/After**
```java
// Before
  private def assignment(assignedPartitions: List[Int]): Array[Byte] = {
        assignedPartitions.map(new TopicPartition("foo", _)).asJava

// After
  /**
   * The test method checks the following scenario:
   * 1. Creating a classic group with member 1, whose assignment has non-empty user data.
   * 2. Member 2 using consumer protocol joins. The group cannot be upgraded and the join is
   *    rejected.
   * 3. Member 1 leaves.
   * 4. Member 2 using consumer protocol joins. The group is upgraded.
   */
  @ClusterTest(
    serverProperties = Array(
      new ClusterConfigProperty(key = GroupCoordinatorConfig.OFFSETS_TOPIC_PARTITIONS_CONFIG, value = "1"),
      new ClusterConfigProperty(key = GroupCoordinatorConfig.OFFSETS_TOPIC_REPLICATION_FACTOR_CONFIG, value = "1"),
```

**How to spot**
When reviewing group-coordinator/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-18134; disallow group upgrades when custom assignors are used (#18046).

**Test**
Test modified in ConsumerProtocolMigrationTest.scala covering the corrected behaviour.
