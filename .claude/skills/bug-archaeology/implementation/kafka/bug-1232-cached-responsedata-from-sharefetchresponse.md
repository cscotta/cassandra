**Commit**: ebb62812d9 · **Ticket**: KAFKA-19074
**Subsystem**: clients/src
**Category**: Logic error in condition

**Root cause**
Jira: https://issues.apache.org/jira/browse/KAFKA-19074

Similar fix https://github.com/apache/kafka/pull/16532

https://github.com/apache/kafka/commit/2b8aff58b575c199ee8372e5689420c9d77357a5
make it accept input to return "partial" data. The content of output is based on the input but we cache the output ... It will return same output even though we pass different input. That is
a potential bug.

**Before/After**
```java
// Before
    private volatile LinkedHashMap<TopicIdPartition, ShareFetchResponseData.PartitionData> responseData = null;

        if (responseData == null) {
            synchronized (this) {
                // Assigning the lazy-initialized `responseData` in the last step
                // to avoid other threads accessing a half-initialized object.
                if (responseData == null) {
                    final LinkedHashMap<TopicIdPartition, ShareFetchResponseData.PartitionData> responseDataTmp = new LinkedHashMap<>();
                    data.responses().forEach(topicResponse -> {
                        String name = topicNames.get(topicResponse.topicId());
                        if (name != null) {
                            topicResponse.partitions().forEach(partitionData -> responseDataTmp.put(new TopicIdPartition(topicResponse.topicId(),

// After
        final LinkedHashMap<TopicIdPartition, ShareFetchResponseData.PartitionData> responseData = new LinkedHashMap<>();
        data.responses().forEach(topicResponse -> {
            String name = topicNames.get(topicResponse.topicId());
            if (name != null) {
                topicResponse.partitions().forEach(partitionData -> responseData.put(new TopicIdPartition(topicResponse.topicId(),
                        new TopicPartition(name, partitionData.partitionIndex())), partitionData));
        });
import org.apache.kafka.common.compress.Compression;
import org.apache.kafka.common.message.ShareFetchResponseData;
import org.apache.kafka.common.protocol.Errors;
import org.apache.kafka.common.record.MemoryRecords;
import org.apache.kafka.common.record.MemoryRecordsBuilder;
```

**How to spot**
When reviewing clients/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-19074 remove the cached responsedata from sharefetchresponse (#19357).

**Test**
Test modified in ShareFetchTest.java covering the corrected behaviour.
