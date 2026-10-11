**Commit**: 017692e86c · **Ticket**: KAFKA-18887
**Subsystem**: clients/src
**Category**: Resource leak

**Root cause**
Implement Admin API extensions beyond list/describe group (delete group, offset-related APIs). * adds methods for describing and manipulating offsets, as described in KIP-1071
* adds corresponding unit tests

These are doing the exact same thing as the corresponding consumer group counter-parts. Reviewers: Lucas Brutschy <lbrutschy@confluent.io>

**Before/After**
```java
// Before
// (no removals)

// After
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

```

**How to spot**
When reviewing clients/src code, look for resource leak patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-18887: implement streams admin apis (#19049).

**Test**
Test modified in DeleteStreamsGroupOffsetsOptions.java covering the corrected behaviour.
