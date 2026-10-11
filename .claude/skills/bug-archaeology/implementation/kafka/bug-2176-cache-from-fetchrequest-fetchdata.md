**Commit**: b917b85e62 · **Ticket**: KAFKA-17817
**Subsystem**: clients/src
**Category**: Race condition / missing synchronization

**Root cause**
Reviewers: Chia-Ping Tsai <chia7712@gmail.com>

**Before/After**
```java
// Before
    private volatile LinkedHashMap<TopicIdPartition, PartitionData> fetchData = null;
        if (fetchData == null) {
            synchronized (this) {
                if (fetchData == null) {
                    // Assigning the lazy-initialized `fetchData` in the last step
                    // to avoid other threads accessing a half-initialized object.
                    final LinkedHashMap<TopicIdPartition, PartitionData> fetchDataTmp = new LinkedHashMap<>();
                    final short version = version();
                    data.topics().forEach(fetchTopic -> {
                        String name;
                        if (version < 13) {
                            name = fetchTopic.topic(); // can't be null

// After
        final LinkedHashMap<TopicIdPartition, PartitionData> fetchData = new LinkedHashMap<>();
        final short version = version();
        data.topics().forEach(fetchTopic -> {
            String name;
            if (version < 13) {
                name = fetchTopic.topic(); // can't be null
            } else {
                name = topicNames.get(fetchTopic.topicId());
            fetchTopic.partitions().forEach(fetchPartition ->
                // Topic name may be null here if the topic name was unable to be resolved using the topicNames map.
                fetchData.put(new TopicIdPartition(fetchTopic.topicId(), new TopicPartition(name, fetchPartition.partition())),
                    new PartitionData(
```

**How to spot**
When reviewing clients/src code, look for race condition / missing synchronization patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-17817 remove cache from fetchrequest#fetchdata (#17535).

**Test**
Test modified in FetchRequestTest.java covering the corrected behaviour.
