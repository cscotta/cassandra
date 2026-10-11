**Commit**: 278a93c45d · **Ticket**: KAFKA-18901
**Subsystem**: group-coordinator/src
**Category**: Logic error in condition

**Root cause**
The current homogeneous SimpleAssignor for share groups is not very good
at revoking partitions which have previously been assigned when the
number of members increases. This PR improves the situation. It also fixes the sorting of assignments in `kafka-consumer-groups.sh`
and `kafka-share-groups.sh` so that it sorts partition indices
numerically instead of alphabetically. It also adds the missing number
of partitions column for share groups.

**Before/After**
```java
// Before
 * A simple partition assignor that assigns partitions of the subscribed topics based on the rules defined in KIP-932 to different members.
            return assignHomogenous(groupSpec, subscribedTopicDescriber);
    private GroupAssignment assignHomogenous(
     * @param groupSpec - The group metadata specifications.
     * @param groupSpec - The group metadata specifications.
     * @param subscribedTopicIds - The set of all the subscribed topic ids for the group.
     * @param targetPartitions - The list of all topic partitions that need assignment.
     * @param currentAssignment - The current assignment for subscribed topic partitions to memberIds.
        Map<TopicIdPartition, List<String>> newAssignment = new HashMap<>();

        // Step 1: Hash member IDs to topic partitions.
        memberHashAssignment(targetPartitions, groupSpec.memberIds(), newAssignment);

// After
import java.util.ListIterator;
 * A simple partition assignor for share groups that assigns partitions of the subscribed topics
 * based on the rules defined in KIP-932 to different members. It is not rack-aware.
 * <p>
 * Assignments are done according to the following principles:
 * <ol>
 *   <li>Balance:          Ensure partitions are distributed equally among all members.
 *                         The difference in assignments sizes between any two members
 *                         should not exceed one partition.</li>
 *   <li>Stickiness:       Minimize partition movements among members by retaining
 *                         as much of the existing assignment as possible.</li>
 * </ol>
```

**How to spot**
When reviewing group-coordinator/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-18901: [1/n] improved homogeneous simpleassignor (#19142).

**Test**
Test modified in SimpleAssignorTest.java covering the corrected behaviour.
