**Commit**: 4f28973bd1 · **Ticket**: KAFKA-18827
**Subsystem**: clients/src
**Category**: Logic error in condition

**Root cause**
In this PR, we have added the share coordinator and KafkaApis side impl
of the intialize share group state RPC. ref:
https://cwiki.apache.org/confluence/display/KAFKA/KIP-932%3A+Queues+for+Kafka#KIP932:QueuesforKafka-InitializeShareGroupStateAPI

Reviewers: Andrew Schofield <aschofield@confluent.io>

**Before/After**
```java
// Before
            this(data, false);
                topicResult -> results.add(new InitializeShareGroupStateResponseData.InitializeStateResult()
                        .setTopicId(topicResult.topicId())
                        .setPartitions(topicResult.partitions().stream()
                                .map(partitionData -> new InitializeShareGroupStateResponseData.PartitionResult()
                                        .setPartition(partitionData.partition())
                                        .setErrorCode(Errors.forException(e).code()))
                                .collect(Collectors.toList()))));
                .setResults(results));
                new InitializeShareGroupStateRequestData(new ByteBufferAccessor(buffer), version),
                version
                result -> result.partitions().forEach(

// After
            this(data, true);
            topicResult -> results.add(new InitializeShareGroupStateResponseData.InitializeStateResult()
                .setTopicId(topicResult.topicId())
                .setPartitions(topicResult.partitions().stream()
                    .map(partitionData -> new InitializeShareGroupStateResponseData.PartitionResult()
                        .setPartition(partitionData.partition())
                        .setErrorCode(Errors.forException(e).code()))
                    .collect(Collectors.toList()))));
            .setResults(results));
            new InitializeShareGroupStateRequestData(new ByteBufferAccessor(buffer), version),
            version
import org.apache.kafka.common.Uuid;
```

**How to spot**
When reviewing clients/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-18827: initialize share state, share coordinator impl. [1/n] (#18968).

**Test**
Test modified in KafkaApisTest.scala covering the corrected behaviour.
