**Commit**: 5608b5cc3a · **Ticket**: KAFKA-16684
**Subsystem**: clients/src
**Category**: Logic error in condition

**Root cause**
The response data should change accordingly to the input, however
with the current design, it will not change even if the input changes. We remove this cache logic to avoid returning wrong data. Reviewers: Chia-Ping Tsai <chia7712@gmail.com>, Apoorv Mittal <apoorvmittal10@gmail.com>, Igor Soarez <soarez@apple.com>

**Before/After**
```java
// Before
    // we build responseData when needed.
    private volatile LinkedHashMap<TopicPartition, FetchResponseData.PartitionData> responseData = null;
        if (responseData == null) {
            synchronized (this) {
                if (responseData == null) {
                    // Assigning the lazy-initialized `responseData` in the last step
                    // to avoid other threads accessing a half-initialized object.
                    final LinkedHashMap<TopicPartition, FetchResponseData.PartitionData> responseDataTmp =
                            new LinkedHashMap<>();
                    data.responses().forEach(topicResponse -> {
                        String name;
                        if (version < 13) {

// After
        final LinkedHashMap<TopicPartition, FetchResponseData.PartitionData> responseData = new LinkedHashMap<>();
        data.responses().forEach(topicResponse -> {
            String name;
            if (version < 13) {
                name = topicResponse.topic();
            } else {
                name = topicNames.get(topicResponse.topicId());
            if (name != null) {
                topicResponse.partitions().forEach(partition ->
                    responseData.put(new TopicPartition(name, partition.partitionIndex()), partition));
            }
        });
```

**How to spot**
When reviewing clients/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-16684: remove caching in fetchresponse.responsedata (#16532).

**Test**
Test modified in FetcherTest.java covering the corrected behaviour.
