**Commit**: 4dd0bcbde8 · **Ticket**: KAFKA-18383
**Subsystem**: core/src
**Category**: Logic error in condition

**Root cause**
Reviewers: Chia-Ping Tsai <chia7712@gmail.com>

**Before/After**
```java
// Before
  val brokerIdGenerationEnable: Boolean = getBoolean(ServerConfigs.BROKER_ID_GENERATION_ENABLE_CONFIG)
  val maxReservedBrokerId: Int = getInt(ServerConfigs.RESERVED_BROKER_MAX_ID_CONFIG)
    defaults.setProperty(ServerConfigs.BROKER_ID_GENERATION_ENABLE_CONFIG, "false")
    defaults.setProperty(ServerConfigs.RESERVED_BROKER_MAX_ID_CONFIG, "1")
    assertEquals(false, config.brokerIdGenerationEnable)
    assertEquals(1, config.maxReservedBrokerId)
  @Test
  def testAcceptsLargeNodeIdForRaftBasedCase(): Unit = {
    // Generation of Broker IDs is not supported when using Raft-based controller quorums,
    // so pick a broker ID greater than reserved.broker.max.id, which defaults to 1000,
    // and make sure it is allowed despite broker.id.generation.enable=true (true is the default)
    val largeBrokerId = 2000

// After
  def testRejectsNegativeNodeId(): Unit = {
                to specify the broker id auto generation and control the broker id generation process.
```

**How to spot**
When reviewing core/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-18383 remove reserved.broker.max.id and broker.id.generation.enable (#18478).

**Test**
Test modified in KafkaConfigTest.scala covering the corrected behaviour.
