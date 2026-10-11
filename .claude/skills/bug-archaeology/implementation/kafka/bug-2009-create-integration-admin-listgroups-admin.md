**Commit**: 078d34f39d · **Ticket**: KAFKA-17910
**Subsystem**: core/src
**Category**: Logic error in condition

**Root cause**
Reviewers: Andrew Schofield <aschofield@confluent.io>, Chia-Ping Tsai <chia7712@gmail.com>

**Before/After**
```java
// Before
// (no removals)

// After
  @ParameterizedTest
  @ValueSource(strings = Array("kraft+kip932"))
  def testListGroups(quorum: String): Unit = {
    val classicGroupId = "classic_group_id"
    val consumerGroupId = "consumer_group_id"
    val shareGroupId = "share_group_id"
    val simpleGroupId = "simple_group_id"
    val testTopicName = "test_topic"

    consumerConfig.put(ConsumerConfig.GROUP_PROTOCOL_CONFIG, GroupProtocol.CLASSIC.name)
    val classicGroupConfig = new Properties(consumerConfig)
    classicGroupConfig.put(ConsumerConfig.GROUP_ID_CONFIG, classicGroupId)
```

**How to spot**
When reviewing core/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-17910 create integration tests for admin.listgroups and admin.describeclassicgroups (#17712).

**Test**
Test modified in PlaintextAdminIntegrationTest.scala covering the corrected behaviour.
