**Commit**: 28c53ba09a · **Ticket**: KAFKA-19453
**Subsystem**: group-coordinator/src
**Category**: Logic error in condition

**Root cause**
* When a `ShareGroup*` record is replayed in group
metadata manager, there is a call to check if the group exists. If the
group does not exist - we are throwing an exception which is
unnecessary. * In this PR, we have added check to ignore this exception. * New test to validate the logic has been added.

**Before/After**
```java
// Before
        ShareGroup shareGroup = getOrMaybeCreatePersistedShareGroup(groupId, value != null);
            ShareGroupMember oldMember = shareGroup.getOrMaybeCreateMember(memberId, true);
            ShareGroupMember oldMember = shareGroup.getOrMaybeCreateMember(memberId, false);
            ShareGroup shareGroup = getOrMaybeCreatePersistedShareGroup(groupId, true);
            ShareGroup shareGroup = getOrMaybeCreatePersistedShareGroup(groupId, false);
        ShareGroup group = getOrMaybeCreatePersistedShareGroup(groupId, false);
        ShareGroup group = getOrMaybeCreatePersistedShareGroup(groupId, false);
        ShareGroup group = getOrMaybeCreatePersistedShareGroup(groupId, false);
        ShareGroupMember oldMember = group.getOrMaybeCreateMember(memberId, false);

// After
        ShareGroup shareGroup;
        ShareGroupMember oldMember;
        try {
            shareGroup = getOrMaybeCreatePersistedShareGroup(groupId, value != null);
            oldMember = shareGroup.getOrMaybeCreateMember(memberId, value != null);
        } catch (GroupIdNotFoundException ex) {
            log.debug("ShareGroupMemberMetadata tombstone without group - {}", groupId, ex);
            return;
        } catch (UnknownMemberIdException ex) {
            log.debug("ShareGroupMemberMetadata tombstone for groupId - {} without member - {}", groupId, memberId, ex);
            return;
        }
```

**How to spot**
When reviewing group-coordinator/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-19453: ignore group not found in share group record replay. (#20076).

**Test**
Test modified in GroupMetadataManagerTest.java covering the corrected behaviour.
