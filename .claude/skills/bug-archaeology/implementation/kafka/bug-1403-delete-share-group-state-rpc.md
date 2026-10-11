**Commit**: c2cb543a1e · **Ticket**: KAFKA-18629
**Subsystem**: checkstyle/import-control-group-coordinator.xml
**Category**: Logic error in condition

**Root cause**
* In this PR, we have added GC side impl to call the delete state share
coord RPC using the persister. * We will be using the existing `GroupCoordinatorService.deleteGroups`. The logic will be modified as follows:
* After sanitization, we will call a new
`runtime.scheduleWriteOperation` (not read for consistency) with
callback `GroupCoordinatorShard.sharePartitions`. This will return a Map
of share partitions of the groups which are of SHARE type.

**Before/After**
```java
// Before
            CompletableFuture<DeleteGroupsResponseData.DeletableGroupResultCollection> future =
                runtime.scheduleWriteOperation(
                    "delete-groups",
                    topicPartition,
                    Duration.ofMillis(config.offsetCommitTimeoutMs()),
                    coordinator -> coordinator.deleteGroups(context, groupList)
                ).exceptionally(exception -> handleOperationException(
                    "delete-groups",
                    groupList,
                    exception,
                    (error, __) -> DeleteGroupsRequest.getErrorResultCollection(groupList, error),
                    log

// After
            <allow pkg="org.apache.kafka.metadata" />
import org.apache.kafka.server.share.persister.DeleteShareGroupStateParameters;
import org.apache.kafka.server.share.persister.DeleteShareGroupStateResult;
import org.apache.kafka.server.share.persister.PartitionErrorData;
import org.apache.kafka.server.share.persister.PartitionFactory;
import org.apache.kafka.server.share.persister.TopicData;
import java.util.HashSet;
import java.util.Set;
@SuppressWarnings({"ClassDataAbstractionCoupling"})
            CompletableFuture<DeleteGroupsResponseData.DeletableGroupResultCollection> future = deleteShareGroups(topicPartition, groupList).thenCompose(groupErrMap -> {
                DeleteGroupsResponseData.DeletableGroupResultCollection collection = new DeleteGroupsResponseData.DeletableGroupResultCollection();
                List<String> retainedGroupIds = deleteCandidateGroupIds(groupErrMap, groupList, collection);
```

**How to spot**
When reviewing checkstyle/import-control-group-coordinator.xml code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-18629: delete share group state rpc group coordinator impl. [3/n] (#18848).

**Test**
Test modified in GroupCoordinatorServiceTest.java covering the corrected behaviour.
