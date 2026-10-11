**Commit**: f32932cc25 · **Ticket**: KAFKA-18629
**Subsystem**: clients/src
**Category**: Logic error in condition

**Root cause**
Reviewers: Christo Lolov <lolovc@amazon.com>, Andrew Schofield <aschofield@confluent.io>

**Before/After**
```java
// Before
    // TODO: Implement the DeleteShareGroupStateRequest handling
    requestHelper.sendMaybeThrottle(request, deleteShareGroupStateRequest.getErrorResponse(Errors.UNSUPPORTED_VERSION.exception))
    CompletableFuture.completedFuture[Unit](())

// After
import org.apache.kafka.common.Uuid;
import java.util.Collections;
import java.util.List;

    public static DeleteShareGroupStateResponseData toResponseData(Uuid topicId, int partitionId) {
        return new DeleteShareGroupStateResponseData()
            .setResults(List.of(
                new DeleteShareGroupStateResponseData.DeleteStateResult()
                    .setTopicId(topicId)
                    .setPartitions(List.of(
                        new DeleteShareGroupStateResponseData.PartitionResult()
                            .setPartition(partitionId)))));
```

**How to spot**
When reviewing clients/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-18629: delete share group state impl [1/n] (#18712).

**Test**
Test modified in KafkaApisTest.scala covering the corrected behaviour.
