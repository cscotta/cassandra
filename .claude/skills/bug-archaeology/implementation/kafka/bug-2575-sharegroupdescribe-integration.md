**Commit**: f47b526eb4 · **Ticket**: KAFKA-17289
**Subsystem**: core/src
**Category**: Resource leak

**Root cause**
Introduces a simple integration test for ShareGroupDescribe. Reviewers: Manikumar Reddy <manikumar.reddy@gmail.com>

**Before/After**
```java
// Before
class ConsumerGroupDescribeRequestsTest(cluster: ClusterInstance) extends GroupCoordinatorBaseRequestTest(cluster) {
import org.apache.kafka.common.message.{ConsumerGroupDescribeRequestData, ConsumerGroupDescribeResponseData, ConsumerGroupHeartbeatRequestData, ConsumerGroupHeartbeatResponseData, DeleteGroupsRequestData, DeleteGroupsResponseData, DescribeGroupsRequestData, DescribeGroupsResponseData, HeartbeatRequestData, HeartbeatResponseData, JoinGroupRequestData, JoinGroupResponseData, LeaveGroupResponseData, ListGroupsRequestData, ListGroupsResponseData, OffsetCommitRequestData, OffsetCommitResponseData, OffsetDeleteRequestData, OffsetDeleteResponseData, OffsetFetchResponseData, SyncGroupRequestData, SyncGroupResponseData}
import org.apache.kafka.common.requests.{AbstractRequest, AbstractResponse, ConsumerGroupDescribeRequest, ConsumerGroupDescribeResponse, ConsumerGroupHeartbeatRequest, ConsumerGroupHeartbeatResponse, DeleteGroupsRequest, DeleteGroupsResponse, DescribeGroupsRequest, DescribeGroupsResponse, HeartbeatRequest, HeartbeatResponse, JoinGroupRequest, JoinGroupResponse, LeaveGroupRequest, LeaveGroupResponse, ListGroupsRequest, ListGroupsResponse, OffsetCommitRequest, OffsetCommitResponse, OffsetDeleteRequest, OffsetDeleteResponse, OffsetFetchRequest, OffsetFetchResponse, SyncGroupRequest, SyncGroupResponse}

// After
    val includeAuthorizedOperations = shareGroupDescribeRequest.data.includeAuthorizedOperations
          if (includeAuthorizedOperations) {
            results.forEach { groupResult =>
              if (groupResult.errorCode == Errors.NONE.code) {
                groupResult.setAuthorizedOperations(authHelper.authorizedOperations(
                  request,
                  new Resource(ResourceType.GROUP, groupResult.groupId)
                ))
              }
            }
          }

```

**How to spot**
When reviewing core/src code, look for resource leak patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-17289: sharegroupdescribe integration test (#16846).

**Test**
Test modified in ConsumerGroupDescribeRequestTest.scala covering the corrected behaviour.
