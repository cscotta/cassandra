**Commit**: d7a456ed5d · **Ticket**: KAFKA-17543
**Subsystem**: core/src
**Category**: Logic error in condition

**Root cause**
Reviewers: Chia-Ping Tsai <chia7712@gmail.com>, David Arthur <mumrah@gmail.com>

**Before/After**
```java
// Before
        // Currently, we are migrating from ZooKeeper to KRaft. If broker.id.generation.enable is set to true,
        // we must ensure that the nodeId synchronizes with the broker.id to prevent the nodeId from being -1,
        // which would result in a failure during the migration.


// After
        if (migrationEnabled) {
          require(brokerId != -1, "broker id generation is incompatible with migration to ZK. Please disable it before enabling migration")
        }
        require(brokerId >= 0,
          "broker broker.id.generation.enable is incompatible with migration to ZK. Please disable it before enabling migration")
  @Test
  def testMigrationCannotBeEnabledWithBrokerIdGeneration(): Unit = {
    val props = TestUtils.createBrokerConfig(-1, TestUtils.MockZkConnect, port = TestUtils.MockZkPort, logDirCount = 2)
    props.setProperty(KRaftConfigs.MIGRATION_ENABLED_CONFIG, "true")
    props.setProperty(QuorumConfig.QUORUM_VOTERS_CONFIG, "3000@localhost:9093")
    props.setProperty(KRaftConfigs.CONTROLLER_LISTENER_NAMES_CONFIG, "CONTROLLER")
    assertEquals(
```

**How to spot**
When reviewing core/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-17543: enforce that broker.id.generation.enable is not used when migrating to kraft (#17192).

**Test**
Test modified in KafkaConfigTest.scala covering the corrected behaviour.
