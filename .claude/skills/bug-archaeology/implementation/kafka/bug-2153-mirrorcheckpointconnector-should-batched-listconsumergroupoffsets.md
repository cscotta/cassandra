**Commit**: 3856644cd6 · **Ticket**: KAFKA-17233
**Subsystem**: connect/mirror
**Category**: Logic error in condition

**Root cause**
Reviewers: Mickael Maison <mickael.maison@gmail.com>, Greg Harris <gharris1727@gmail.com>

**Before/After**
```java
// Before
            Set<String> consumedTopics = listConsumerGroupOffsets(group).keySet().stream()
    Map<TopicPartition, OffsetAndMetadata> listConsumerGroupOffsets(String group)
                () -> sourceAdminClient.listConsumerGroupOffsets(group).partitionsToOffsetAndMetadata().get(),
                () -> String.format("list offsets for consumer group %s on %s cluster", group,
                        config.sourceClusterAlias())
        doReturn(offsets).when(connector).listConsumerGroupOffsets(anyString());
        Map<TopicPartition, OffsetAndMetadata> offsetsForGroup4 = new HashMap<>();
        offsetsForGroup4.put(new TopicPartition("t3", 0), new OffsetAndMetadata(0));

// After
import org.apache.kafka.clients.admin.ListConsumerGroupOffsetsSpec;
        Map<String, Map<TopicPartition, OffsetAndMetadata>> groupToOffsets = listConsumerGroupOffsets(filteredGroups);
            Set<String> consumedTopics = groupToOffsets.get(group).keySet().stream()
    Map<String, Map<TopicPartition, OffsetAndMetadata>> listConsumerGroupOffsets(List<String> groups)
        ListConsumerGroupOffsetsSpec groupOffsetsSpec = new ListConsumerGroupOffsetsSpec();
        Map<String, ListConsumerGroupOffsetsSpec> groupSpecs = groups.stream()
                .collect(Collectors.toMap(group -> group, group -> groupOffsetsSpec));
                () -> sourceAdminClient.listConsumerGroupOffsets(groupSpecs).all().get(),
                () -> String.format("list offsets for consumer groups %s on %s cluster", groups, config.sourceClusterAlias())
import static org.mockito.ArgumentMatchers.anyList;

        Map<String, Map<TopicPartition, OffsetAndMetadata>> groupToOffsets = new HashMap<>();
```

**How to spot**
When reviewing connect/mirror code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-17233: mirrorcheckpointconnector should use batched listconsumergroupoffsets (#17038).

**Test**
Test modified in MirrorCheckpointConnectorTest.java covering the corrected behaviour.
