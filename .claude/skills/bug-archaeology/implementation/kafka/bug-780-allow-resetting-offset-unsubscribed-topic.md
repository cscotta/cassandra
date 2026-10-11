**Commit**: 37e04eca81 · **Ticket**: KAFKA-19662
**Subsystem**: tools/src
**Category**: Logic error in condition

**Root cause**
The `kafka-share-groups.sh` tool checks whether a topic already has a
start-offset in the share group when resetting offsets. This is not
necessary. By removing the check, it is possible to set a start offset
for a topic which has not yet but will be subscribed in the future, thus
initialising the consumption point. There is still a small piece of outstanding work to do with resetting
the offset for a non-existent group which should also create the group.

**Before/After**
```java
// Before
                Set<String> subscribedTopics = offsetsByTopicPartitions.keySet().stream()
                    .map(TopicPartition::topic)
                    .collect(Collectors.toSet());
                Set<String> resetTopics = partitionsToReset.stream()
                    .map(TopicPartition::topic)
                    .collect(Collectors.toSet());
                if (!subscribedTopics.containsAll(resetTopics)) {
                    CommandLineUtils
                        .printErrorAndExit(String.format("Share group '%s' is not subscribed to topic '%s'.",
                            groupId, resetTopics.stream().filter(topic -> !subscribedTopics.contains(topic)).collect(Collectors.joining(", "))));
                    return null;
                }

// After
    public void testAlterShareGroupUnsubscribedTopicSuccess() {
        when(adminClient.listShareGroupOffsets(any())).thenReturn(listShareGroupOffsetsResult);

        AlterShareGroupOffsetsResult alterShareGroupOffsetsResult = mockAlterShareGroupOffsets(adminClient, group);
        TopicPartition tp0 = new TopicPartition(topic, 0);
        Map<TopicPartition, OffsetAndMetadata> partitionOffsets = Map.of(tp0, new OffsetAndMetadata(0L));
        ListOffsetsResult listOffsetsResult = AdminClientTestUtils.createListOffsetsResult(partitionOffsets);
        when(adminClient.listOffsets(any(), any(ListOffsetsOptions.class))).thenReturn(listOffsetsResult);

            List.of(),
```

**How to spot**
When reviewing tools/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-19662: allow resetting offset for unsubscribed topic in kafka-share-groups.sh (#20453).

**Test**
Test modified in ShareGroupCommandTest.java covering the corrected behaviour.
