**Commit**: c8380ae779 · **Ticket**: KAFKA-17750
**Subsystem**: clients/src
**Category**: State not cleaned up

**Root cause**
* Add fields `groupEpoch` and `targetAssignmentEpoch` to `ConsumerGroupDescription.java`. * Add fields `memberEpoch` and `upgraded` to `MemberDescription.java`. * Add assertion to `PlaintextAdminIntegrationTest#testDescribeClassicGroups` to make sure member in classic group returns `upgraded` as `Optional.empty`. * Add new case `testConsumerGroupWithMemberMigration` to `PlaintextAdminIntegrationTest` to make sure migration member has correct `upgraded` value.

**Before/After**
```java
// Before
     * @deprecated Since 4.0. Use {@link #ConsumerGroupDescription(String, boolean, Collection, String, GroupState, Node)}.
     * @deprecated Since 4.0. Use {@link #ConsumerGroupDescription(String, boolean, Collection, String, GroupState, Node, Set)}.
     * @deprecated Since 4.0. Use {@link #ConsumerGroupDescription(String, boolean, Collection, String, GroupType, GroupState, Node, Set)}.
    }

    public ConsumerGroupDescription(String groupId,
                                    boolean isSimpleConsumerGroup,
                                    Collection<MemberDescription> members,
                                    String partitionAssignor,
                                    GroupState groupState,
                                    Node coordinator) {
        this(groupId, isSimpleConsumerGroup, members, partitionAssignor, groupState, coordinator, Collections.emptySet());

// After
import java.util.Optional;
    private final Optional<Integer> groupEpoch;
    private final Optional<Integer> targetAssignmentEpoch;
     * @deprecated Since 4.0. Use {@link #ConsumerGroupDescription(String, boolean, Collection, String, GroupType, GroupState, Node, Set, Optional, Optional)}.
     * @deprecated Since 4.0. Use {@link #ConsumerGroupDescription(String, boolean, Collection, String, GroupType, GroupState, Node, Set, Optional, Optional)}.
     * @deprecated Since 4.0. Use {@link #ConsumerGroupDescription(String, boolean, Collection, String, GroupType, GroupState, Node, Set, Optional, Optional)}.
        this.groupEpoch = Optional.empty();
        this.targetAssignmentEpoch = Optional.empty();
                                    Set<AclOperation> authorizedOperations,
                                    Optional<Integer> groupEpoch,
                                    Optional<Integer> targetAssignmentEpoch) {
        this.groupEpoch = groupEpoch;
```

**How to spot**
When reviewing clients/src code, look for state not cleaned up patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-17750: extend kafka-consumer-groups command line tool to support new consumer group (part 2) (#18034).

**Test**
Test modified in KafkaAdminClientTest.java covering the corrected behaviour.
