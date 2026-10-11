**Commit**: e79f5f0f65 · **Ticket**: KAFKA-19195
**Subsystem**: group-coordinator/src
**Category**: Logic error in condition

**Root cause**
If a streams, share or consumer group is described, all group IDs sent
to all shards of the group coordinator. This change fixes it. It tested
in the unit tests, since it's somewhat inconvenient to test the passed
read operation lambda. Reviewers: David Jacot <djacot@confluent.io>, Andrew Schofield
<aschofield@confluent.io>

**Before/After**
```java
// Before
                    (coordinator, lastCommittedOffset) -> coordinator.consumerGroupDescribe(groupIds, lastCommittedOffset)
                    (coordinator, lastCommittedOffset) -> coordinator.streamsGroupDescribe(groupIds, lastCommittedOffset)
                    (coordinator, lastCommittedOffset) -> coordinator.shareGroupDescribe(groupIds, lastCommittedOffset)
            ArgumentMatchers.any()
        CompletableFuture<Object> describedGroupFuture = new CompletableFuture<>();
            ArgumentMatchers.any()

// After
                    (coordinator, lastCommittedOffset) -> coordinator.consumerGroupDescribe(groupList, lastCommittedOffset)
                    (coordinator, lastCommittedOffset) -> coordinator.streamsGroupDescribe(groupList, lastCommittedOffset)
                    (coordinator, lastCommittedOffset) -> coordinator.shareGroupDescribe(groupList, lastCommittedOffset)
import org.mockito.ArgumentCaptor;
        @SuppressWarnings("unchecked")
        ArgumentCaptor<CoordinatorRuntime.CoordinatorReadOperation<GroupCoordinatorShard, List<ConsumerGroupDescribeResponseData.DescribedGroup>>> readOperationCaptor =
            ArgumentCaptor.forClass(CoordinatorRuntime.CoordinatorReadOperation.class);

            readOperationCaptor.capture()
        CompletableFuture<List<ConsumerGroupDescribeResponseData.DescribedGroup>> describedGroupFuture = new CompletableFuture<>();
            readOperationCaptor.capture()

```

**How to spot**
When reviewing group-coordinator/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-19195: only send the right group id subset to each gc shard (#19548).

**Test**
Test modified in GroupCoordinatorServiceTest.java covering the corrected behaviour.
