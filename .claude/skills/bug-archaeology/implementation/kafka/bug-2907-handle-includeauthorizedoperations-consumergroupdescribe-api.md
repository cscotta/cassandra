**Commit**: 40de07dab5 · **Ticket**: KAFKA-14509
**Subsystem**: core/src
**Category**: Resource leak

**Root cause**
This patch implements the handling of `includeAuthorizedOperations` flag in the ConsumerGroupDescribe API. Reviewers: David Jacot <djacot@confluent.io>

**Before/After**
```java
// Before
import kafka.server.GroupCoordinatorBaseRequestTest
import kafka.test.annotation.{ClusterConfigProperty, ClusterFeature, ClusterTest, ClusterTestDefaults, Type}
        version = version.toShort

// After
    val includeAuthorizedOperations = consumerGroupDescribeRequest.data.includeAuthorizedOperations
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
When reviewing core/src code, look for resource leak patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-14509; [4/4] handle includeauthorizedoperations in consumergroupdescribe api (#16158).

**Test**
Test modified in ConsumerGroupDescribeRequestsTest.scala covering the corrected behaviour.
