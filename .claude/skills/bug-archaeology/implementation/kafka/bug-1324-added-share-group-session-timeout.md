**Commit**: ef2941154e · **Ticket**: KAFKA-18931
**Subsystem**: group-coordinator/src
**Category**: Logic error in condition

**Root cause**
This PR adds `scheduleShareGroupSessionTimeout` for all the persisted
members of a share group when the group coordinator is loaded. Reviewers: Andrew Schofield <aschofield@confluent.io>

**Before/After**
```java
// Before
                    // Nothing for now for the ShareGroup, as no members are persisted.

// After
                    ShareGroup shareGroup = (ShareGroup) group;
                    log.info("Loaded share group {} with {} members.", groupId, shareGroup.members().size());
                    shareGroup.members().forEach((memberId, member) -> {
                        log.debug("Loaded member {} in share group {}.", memberId, groupId);
                        scheduleShareGroupSessionTimeout(groupId, memberId);
                    });
    @Test
    public void testOnLoadedSessionTimeoutExpiration() {
        String groupId = "group";
        Uuid fooTopicId = Uuid.randomUuid();
        String fooTopicName = "foo";
        String memberId = "foo-1";
```

**How to spot**
When reviewing group-coordinator/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-18931: added a share group session timeout task when group coordinator is loaded (#19173).

**Test**
Test modified in GroupMetadataManagerTest.java covering the corrected behaviour.
