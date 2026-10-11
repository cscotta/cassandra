**Commit**: 48a52701b9 · **Ticket**: KAFKA-18424
**Subsystem**: core/src
**Category**: Logic error in condition

**Root cause**
JIRA: KAFKA-18424
`PlaintextAdminIntegrationTest#testConsumerGroups` test too many things. We should split it into smaller units. Reviewers: Chia-Ping Tsai <chia7712@gmail.com>

**Before/After**
```java
// Before
  /**
   * Test the consumer group APIs.
   */
  def testConsumerGroups(groupProtocol: String): Unit = {
      // Verify that initially there are no consumer groups to list.
      val list1 = client.listConsumerGroups()
      assertEquals(0, list1.all().get().size())
      assertEquals(0, list1.errors().get().size())
      assertEquals(0, list1.valid().get().size())
      val testTopicName1 = testTopicName + "1"
      val testTopicName2 = testTopicName + "2"
      val testNumPartitions = 2

// After
  def testListConsumerGroupOffsets(groupProtocol: String): Unit = {
      assertConsumerGroupsIsClean()

      prepareTopics(List(testTopicName), 2)
      prepareRecords(testTopicName)
      val testGroupId = "test_group_id"
      val testClientId = "test_client_id"
      val groupInstances = Set("")
      val topics = Set(testTopicName)

      // We need to disable the auto commit because after the members got removed from group, the offset commit
      // will cause the member rejoining and the test will be flaky (check ConsumerCoordinator#OffsetCommitResponseHandler)
```

**How to spot**
When reviewing core/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-18424: consider splitting plaintextadminintegrationtest#testconsumergroups (#19093).

**Test**
Test modified in PlaintextAdminIntegrationTest.scala covering the corrected behaviour.
