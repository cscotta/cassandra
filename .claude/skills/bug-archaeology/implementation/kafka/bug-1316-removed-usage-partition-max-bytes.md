**Commit**: c07c59ad24 · **Ticket**: KAFKA-18932
**Subsystem**: clients/src
**Category**: Resource leak

**Root cause**
This PR aims to remove the usage of partition max bytes from share fetch
requests. Partition Max Bytes is being defined by
`PartitionMaxBytesStrategy` which was added to the broker as part of PR
https://github.com/apache/kafka/pull/17870

Reviewers: Andrew Schofield <aschofield@confluent.io>, Apoorv Mittal <apoorvmittal10@gmail.com>

**Before/After**
```java
// Before
                fetchConfig.minBytes, fetchConfig.maxBytes, fetchConfig.fetchSize, fetchConfig.maxPollRecords,
import java.util.LinkedHashMap;
import java.util.Objects;
                                          int maxWait, int minBytes, int maxBytes, int fetchSize, int batchSize,
            int ackOnlyPartitionMaxBytes = fetchSize;
                    ackOnlyPartitionMaxBytes = 0;
                            .setPartitionIndex(tip.partition())
                            .setPartitionMaxBytes(fetchSize);
                            .setPartitionIndex(tip.partition())
                            .setPartitionMaxBytes(ackOnlyPartitionMaxBytes);
    private volatile LinkedHashMap<TopicIdPartition, ShareFetchRequest.SharePartitionData> shareFetchData = null;
    public static final class SharePartitionData {

// After
                fetchConfig.minBytes, fetchConfig.maxBytes, fetchConfig.maxPollRecords,
                                          int maxWait, int minBytes, int maxBytes, int batchSize,
                            .setPartitionIndex(tip.partition());
                            .setPartitionIndex(tip.partition());
    private volatile List<TopicIdPartition> shareFetchData = null;
```

**How to spot**
When reviewing clients/src code, look for resource leak patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-18932: removed usage of partition max bytes from share fetch requests (#19148).

**Test**
Test modified in DelayedShareFetchTest.java covering the corrected behaviour.
