**Commit**: 0344f8f5ae · **Ticket**: KAFKA-18273
**Subsystem**: clients/src
**Category**: State not cleaned up

**Root cause**
Reviewers: Manikumar Reddy <manikumar.reddy@gmail.com>

**Before/After**
```java
// Before
                                 Node coordinator) {
        this(groupId, members, groupState, coordinator, Collections.emptySet());
        return Objects.hash(groupId, members, groupState, coordinator, authorizedOperations);
        ShareMemberAssignment assignment
            assignment.equals(that.assignment);
        return Objects.hash(memberId, clientId, host, assignment);

// After
    private final int groupEpoch;
    private final int targetAssignmentEpoch;
                                 Node coordinator,
                                 int groupEpoch,
                                 int targetAssignmentEpoch) {
        this(groupId, members, groupState, coordinator, groupEpoch, targetAssignmentEpoch, Collections.emptySet());
                                 int groupEpoch,
                                 int targetAssignmentEpoch,
        this.groupEpoch = groupEpoch;
        this.targetAssignmentEpoch = targetAssignmentEpoch;
            groupEpoch == that.groupEpoch &&
            targetAssignmentEpoch == that.targetAssignmentEpoch &&
```

**How to spot**
When reviewing clients/src code, look for state not cleaned up patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-18273: kip-1099 verbose display share group options (#18259).

**Test**
Test modified in KafkaAdminClientTest.java covering the corrected behaviour.
