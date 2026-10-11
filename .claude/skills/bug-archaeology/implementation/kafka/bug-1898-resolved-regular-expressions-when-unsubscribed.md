**Commit**: e99561e1f3 · **Ticket**: KAFKA-17593
**Subsystem**: group-coordinator/src
**Category**: Logic error in condition

**Root cause**
This patch does a few things:
1) It cleans up resolved regular expressions when they are unsubscribed from. It covers the regular leave/fenced paths for the new protocol and it also covers the LeaveGroup API as new members could be removed via the admin API. 2) It ensures that tombstones for  resolved regular expressions are generated on the conversion patch from consumer to classic group. 3) It fixes [KAFKA-18116](https://issues.apache.org/jira/browse/KAFKA-18116) because I faced the same issue while working on the LeaveGroup API.

**Before/After**
```java
// Before
    includeAuthorizedOperations: Boolean,
  protected def leaveGroupWithOldProtocol(
    expectedLeaveGroupError: Errors,
    expectedMemberErrors: List[Errors],
  ): Unit = {
    val leaveGroupResponse = connectAndReceive[LeaveGroupResponse](leaveGroupRequest)
    assertEquals(expectedResponseData, leaveGroupResponse.data)
@ClusterTestDefaults(types = Array(Type.KRAFT))
  @ClusterTest(serverProperties = Array(
    new ClusterConfigProperty(key = GroupCoordinatorConfig.OFFSETS_TOPIC_PARTITIONS_CONFIG, value = "1"),
    new ClusterConfigProperty(key = GroupCoordinatorConfig.OFFSETS_TOPIC_REPLICATION_FACTOR_CONFIG, value = "1")
  ))

// After
    includeAuthorizedOperations: Boolean = false,
  protected def classicLeaveGroup(
  ): LeaveGroupResponseData = {
    connectAndReceive[LeaveGroupResponse](leaveGroupRequest).data
  }

  protected def leaveGroupWithOldProtocol(
    groupId: String,
    memberIds: List[String],
    groupInstanceIds: List[String] = null,
    expectedLeaveGroupError: Errors,
    expectedMemberErrors: List[Errors],
```

**How to spot**
When reviewing group-coordinator/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-17593; [10/n] remove resolved regular expressions when unsubscribed (#17976).

**Test**
Test modified in GroupCoordinatorBaseRequestTest.scala covering the corrected behaviour.
