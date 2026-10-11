**Commit**: 96816a3ed7 · **Ticket**: KAFKA-16742
**Subsystem**: group-coordinator/src
**Category**: Logic error in condition

**Root cause**
Share group describe functionality for KIP-932

Reviewers:  Andrew Schofield <aschofield@confluent.io>,  Manikumar Reddy <manikumar.reddy@gmail.com>

**Before/After**
```java
// Before
        // TODO: Implement this method as part of KIP-932.
        throw new UnsupportedOperationException();

// After
import org.apache.kafka.common.message.ShareGroupDescribeResponseData;
import org.apache.kafka.common.requests.ShareGroupDescribeRequest;
        if (!isActive.get()) {
            return CompletableFuture.completedFuture(ShareGroupDescribeRequest.getErrorDescribedGroupList(
                groupIds,
                Errors.COORDINATOR_NOT_AVAILABLE
            ));
        }

        final List<CompletableFuture<List<ShareGroupDescribeResponseData.DescribedGroup>>> futures =
            new ArrayList<>(groupIds.size());
        final Map<TopicPartition, List<String>> groupsByTopicPartition = new HashMap<>();
```

**How to spot**
When reviewing group-coordinator/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-16742: add share group describe in coordinator (kip-932) (#16797).

**Test**
Test modified in GroupCoordinatorServiceTest.java covering the corrected behaviour.
