**Commit**: 410065a65d · **Ticket**: KAFKA-18517
**Subsystem**: core/src
**Category**: Logic error in condition

**Root cause**
Reviewers: Andrew Schofield <aschofield@confluent.io>, Kirk True <ktrue@confluent.io>

**Before/After**
```java
// Before
import org.junit.jupiter.api.{AfterEach, Disabled, Test, TestInfo}
  @MethodSource(Array("getTestQuorumAndGroupProtocolParametersClassicGroupProtocolOnly"))
  @MethodSource(Array("getTestQuorumAndGroupProtocolParametersClassicGroupProtocolOnly"))
  @MethodSource(Array("getTestQuorumAndGroupProtocolParametersClassicGroupProtocolOnly"))
    checkCloseWithClusterFailure(numRecords, "group4", "group5")
  private def checkCloseWithClusterFailure(numRecords: Int, group1: String, group2: String): Unit = {
    this.consumerConfig.setProperty(ConsumerConfig.SESSION_TIMEOUT_MS_CONFIG, "5000")
    this.consumerConfig.setProperty(ConsumerConfig.HEARTBEAT_INTERVAL_MS_CONFIG, "1000")
  @Test
  def testRollingBrokerRestartsWithSmallerMaxGroupSizeConfigDisruptsBigGroup(): Unit = {

// After
import org.junit.jupiter.api.{AfterEach, Disabled, TestInfo}

    // Tests will run for CONSUMER and CLASSIC group protocol, so set the group max size property
    // required for each.
    GroupCoordinatorConfig.CONSUMER_GROUP_MAX_SIZE_CONFIG -> maxGroupSize.toString,

  @MethodSource(Array("getTestQuorumAndGroupProtocolParametersAll"))
  @MethodSource(Array("getTestQuorumAndGroupProtocolParametersAll"))
  @MethodSource(Array("getTestQuorumAndGroupProtocolParametersAll"))
    checkCloseWithClusterFailure(numRecords, "group4", "group5", groupProtocol)
  private def checkCloseWithClusterFailure(numRecords: Int, group1: String, group2: String,
                                           groupProtocol: String): Unit = {
```

**How to spot**
When reviewing core/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-18517: enable consumerbouncetest to run for new async consumer (#18532).

**Test**
Test modified in ConsumerBounceTest.scala covering the corrected behaviour.
