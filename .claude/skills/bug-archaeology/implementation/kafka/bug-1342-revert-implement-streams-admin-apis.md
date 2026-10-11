**Commit**: d86cb59790 · **Ticket**: KAFKA-18887
**Subsystem**: clients/src
**Category**: Resource leak

**Root cause**
This reverts commit 017692e86c5e40402312ebdb4bdab82face30181.

**Before/After**
```java
// Before
    /**
     * List the Streams group offsets available in the cluster for the specified Streams groups.
     *
     * <em>Note</em>: this method effectively does the same as the corresponding consumer group method {@link Admin#listConsumerGroupOffsets} does.
     *
     * @param groupSpecs Map of Streams group ids to a spec that specifies the topic partitions of the group to list offsets for.
     *
     * @param options The options to use when listing the Streams group offsets.
     * @return The ListStreamsGroupOffsetsResult
     */
    ListStreamsGroupOffsetsResult listStreamsGroupOffsets(Map<String, ListStreamsGroupOffsetsSpec> groupSpecs, ListStreamsGroupOffsetsOptions options);


// After
// (no additions)
```

**How to spot**
When reviewing clients/src code, look for resource leak patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: revert "kafka-18887: implement streams admin apis (#19049)".

**Test**
Test modified in DeleteStreamsGroupOffsetsOptions.java covering the corrected behaviour.
