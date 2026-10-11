**Commit**: 6a6b80215d · **Ticket**: KAFKA-16717
**Subsystem**: clients/src
**Category**: Logic error in condition

**Root cause**
KAFKA-16720 aims to add the support for the AlterShareGroupOffsets AdminClient. Key Changes in the PR:

1. Added handing of alterShareGroupOffsets() in KafkaAdminClient and introduce AlterShareGroupOffsetRequest/AlterShareGroupOffsetResponse/AlterShareGroupOffsetsOptions classes. 2.

**Before/After**
```java
// Before
// (no removals)

// After
    /**
     * Alters offsets for the specified group. In order to succeed, the group must be empty.
     *
     * <p>This operation is not transactional, so it may succeed for some partitions while fail for others.
     *
     * @param groupId The group for which to alter offsets.
     * @param offsets A map of offsets by partition. Partitions not specified in the map are ignored.
     * @param options The options to use when altering the offsets.
     * @return The AlterShareGroupOffsetsResult.
     */
    AlterShareGroupOffsetsResult alterShareGroupOffsets(String groupId, Map<TopicPartition, Long> offsets, AlterShareGroupOffsetsOptions options);

```

**How to spot**
When reviewing clients/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-16717 [1/2]: add adminclient.altersharegroupoffsets (#18819).

**Test**
Test modified in KafkaAdminClientTest.java covering the corrected behaviour.
