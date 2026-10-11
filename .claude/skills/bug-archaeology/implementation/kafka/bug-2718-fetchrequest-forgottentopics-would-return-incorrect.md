**Commit**: 20ee83c462 · **Ticket**: KAFKA-17102
**Subsystem**: clients/src
**Category**: Race condition / missing synchronization

**Root cause**
Reviewers: Chia-Ping Tsai <chia7712@gmail.com>

**Before/After**
```java
// Before
    private volatile List<TopicIdPartition> toForget = null;
        if (toForget == null) {
            synchronized (this) {
                if (toForget == null) {
                    // Assigning the lazy-initialized `toForget` in the last step
                    // to avoid other threads accessing a half-initialized object.
                    final List<TopicIdPartition> toForgetTmp = new ArrayList<>();
                    data.forgottenTopicsData().forEach(forgottenTopic -> {
                        String name;
                        if (version() < 13) {
                            name = forgottenTopic.topic(); // can't be null
                        } else {

// After
        final List<TopicIdPartition> toForget = new ArrayList<>();
        data.forgottenTopicsData().forEach(forgottenTopic -> {
            String name;
            if (version() < 13) {
                name = forgottenTopic.topic(); // can't be null
            } else {
                name = topicNames.get(forgottenTopic.topicId());
            // Topic name may be null here if the topic name was unable to be resolved using the topicNames map.
            forgottenTopic.partitions().forEach(partitionId -> toForget.add(new TopicIdPartition(forgottenTopic.topicId(), new TopicPartition(name, partitionId))));
        });
    @ParameterizedTest
    @MethodSource("fetchVersions")
```

**How to spot**
When reviewing clients/src code, look for race condition / missing synchronization patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-17102 fetchrequest#forgottentopics would return incorrect data (#16557).

**Test**
Test modified in FetchRequestTest.java covering the corrected behaviour.
