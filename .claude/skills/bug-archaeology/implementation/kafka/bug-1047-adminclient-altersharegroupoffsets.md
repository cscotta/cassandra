**Commit**: b44bfca408 · **Ticket**: KAFKA-16717
**Subsystem**: clients/src
**Category**: Logic error in condition

**Root cause**
[KAFKA-16720](https://issues.apache.org/jira/browse/KAFKA-16720) aims to
finish the AlterShareGroupOffsets RPC. Reviewers: Andrew Schofield <aschofield@confluent.io>

---------

Co-authored-by: jimmy <wangzhiwang@qq.com>

**Before/After**
```java
// Before
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

    public AlterShareGroupOffsetsRequest(AlterShareGroupOffsetsRequestData data, short version) {
        List<AlterShareGroupOffsetsResponseData.AlterShareGroupOffsetsResponseTopic> results = new ArrayList<>();
        data.topics().forEach(
            topicResult -> results.add(new AlterShareGroupOffsetsResponseData.AlterShareGroupOffsetsResponseTopic()
                .setTopicName(topicResult.topicName())
                .setPartitions(topicResult.partitions().stream()
                    .map(partitionData -> new AlterShareGroupOffsetsResponseData.AlterShareGroupOffsetsResponsePartition()
                        .setPartitionIndex(partitionData.partitionIndex())

// After
    private AlterShareGroupOffsetsRequest(AlterShareGroupOffsetsRequestData data, short version) {
        Errors error = Errors.forException(e);
        return new AlterShareGroupOffsetsResponse(getErrorResponse(throttleTimeMs, error));
    }

    public static AlterShareGroupOffsetsResponseData getErrorResponse(int throttleTimeMs, Errors error) {
        return new AlterShareGroupOffsetsResponseData()
            .setThrottleTimeMs(throttleTimeMs)
            .setErrorCode(error.code())
            .setErrorMessage(error.message());
    }

```

**How to spot**
When reviewing clients/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-16717 [2/n]: add adminclient.altersharegroupoffsets (#18929).

**Test**
Test modified in KafkaApisTest.scala covering the corrected behaviour.
