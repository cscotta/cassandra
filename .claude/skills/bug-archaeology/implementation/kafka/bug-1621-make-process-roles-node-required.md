**Commit**: 14daa23b59 · **Ticket**: KAFKA-18331
**Subsystem**: core/src
**Category**: Logic error in condition

**Root cause**
In 4.0, there is no ZK mode and both of these configs are required in kraft mode. Reviewers: Ismael Juma <ismael@juma.me.uk>

**Before/After**
```java
// Before
    if (requiresZookeeper) {
      if (zkConnect == null) {
        throw new ConfigException(s"Missing required configuration `${ZkConfigs.ZK_CONNECT_CONFIG}` which has no default value.")
      }
      if (brokerIdGenerationEnable) {
        require(brokerId >= -1 && brokerId <= maxReservedBrokerId, "broker.id must be greater than or equal to -1 and not greater than reserved.broker.max.id")
      } else {
        require(brokerId >= 0, "broker.id must be greater than or equal to 0")
      }
    } else {
      // KRaft-based metadata quorum
      if (nodeId < 0) {

// After
  @Test
  def testBrokerRequiredProperties(): Unit = {
    val properties = new Properties()
    assertBadConfigContainingMessage(properties,
      "Missing required configuration \"process.roles\" which has no default value.")

    properties.put(KRaftConfigs.PROCESS_ROLES_CONFIG, "broker")
    assertBadConfigContainingMessage(properties,
      "Missing required configuration \"node.id\" which has no default value.")

    properties.put(KRaftConfigs.NODE_ID_CONFIG, -1)
    assertBadConfigContainingMessage(properties,
```

**How to spot**
When reviewing core/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-18331: make process.roles and node.id required configs (#18414).

**Test**
Test modified in KafkaConfigTest.scala covering the corrected behaviour.
