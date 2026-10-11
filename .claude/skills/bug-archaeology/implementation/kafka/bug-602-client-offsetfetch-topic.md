**Commit**: 5373f2d208 · **Ticket**: KAFKA-15283
**Subsystem**: clients/src
**Category**: Logic error in condition

**Root cause**
Introduce Topic ID in existing OffsetFetch API, the patch only contains
the client side changes. Reviewers: Lianet Magrans <lmagrans@confluent.io>

**Before/After**
```java
// Before
            List<OffsetFetchRequestData.OffsetFetchRequestTopics> topics = requestedPartitions.stream()
                .collect(Collectors.groupingBy(TopicPartition::topic))
                .entrySet()
                .stream()
                .map(entry -> new OffsetFetchRequestData.OffsetFetchRequestTopics()
                        .collect(Collectors.toList())))
                .collect(Collectors.toList());
            OffsetFetchRequest.Builder builder = memberInfo.memberEpoch
                .map(epoch -> OffsetFetchRequest.Builder.forTopicNames(
                    new OffsetFetchRequestData()
                        .setRequireStable(true)
                        .setGroups(List.of(

// After
            Map<String, Uuid> topicIds = metadata.topicIds();
            boolean canUseTopicIds = true;
            List<OffsetFetchRequestData.OffsetFetchRequestTopics> topics = new ArrayList<>();
            Map<String, List<TopicPartition>> tps = requestedPartitions.stream().collect(Collectors.groupingBy(TopicPartition::topic));
            for (Map.Entry<String, List<TopicPartition>> entry : tps.entrySet()) {
                String topic = entry.getKey();
                Uuid topicId = topicIds.getOrDefault(topic, Uuid.ZERO_UUID);
                if (Uuid.ZERO_UUID.equals(topicId)) {
                    canUseTopicIds = false;
                }
                topics.add(new OffsetFetchRequestData.OffsetFetchRequestTopics()
                    .setTopicId(topicId)
```

**How to spot**
When reviewing clients/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-15283:[2/2] client support for offsetfetch with topic id (#19885).

**Test**
Test modified in CommitRequestManagerTest.java covering the corrected behaviour.
