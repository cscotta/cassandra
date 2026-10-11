**Commit**: 7973aa6a39 · **Ticket**: KAFKA-16861
**Subsystem**: group-coordinator/src
**Category**: Logic error in condition

**Root cause**
Fix the bug where the group downgrade to a classic one when a member leaves, even though the consumer group size is still larger than `classicGroupMaxSize`. Reviewers: Chia-Ping Tsai <chia7712@gmail.com>, David Jacot <djacot@confluent.io>

**Before/After**
```java
// Before
// (no removals)

// After
            return false;
    @Test
    public void testNoConversionWhenSizeExceedsClassicMaxGroupSize() throws Exception {
        String groupId = "group-id";
        String nonClassicMemberId = "1";

        List<ConsumerGroupMemberMetadataValue.ClassicProtocol> protocols = Collections.singletonList(
            new ConsumerGroupMemberMetadataValue.ClassicProtocol()
                .setName("range")
                .setMetadata(new byte[0])
        );

```

**How to spot**
When reviewing group-coordinator/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-16861: don't convert to group to classic if the size is larger than group max size. (#16163).

**Test**
Test modified in GroupMetadataManagerTest.java covering the corrected behaviour.
