**Commit**: 98f7da9172 · **Ticket**: KAFKA-16930
**Subsystem**: group-coordinator/src
**Category**: Missing null / bounds check

**Root cause**
Fix the following NPE:

```
java.lang.NullPointerException: Cannot invoke "org.apache.kafka.coordinator.group.assignor.MemberAssignment.targetPartitions()" because the return value of "java.util.Map.get(Object)" is null
	at org.apache.kafka.coordinator.group.assignor.GeneralUniformAssignmentBuilder.canMemberParticipateInReassignment(GeneralUniformAssignmentBuilder.java:248)
	at org.apache.kafka.coordinator.group.assignor.GeneralUniformAssignmentBuilder.balance(GeneralUniformAssignmentBuilder.java:336)
	at org.apache.kafka.coordinator.group.assignor.GeneralUniformAssignmentBuilder.buildAssignment(GeneralUniformAssignmentBuilder.java:157)
	at org.apache.kafka.coordinator.group.assignor.Uniform...

**Before/After**
```java
// Before
        groupSpec.memberIds().forEach(memberId ->
                targetAssignment.put(memberId, new MemberAssignmentImpl(new HashMap<>()));
            })
        );

// After
        groupSpec.memberIds().forEach(memberId -> {
            });
            targetAssignment.put(memberId, new MemberAssignmentImpl(new HashMap<>()));
        });

    @Test
    public void testFirstAssignmentWithTwoMembersIncludingOneWithoutSubscriptions() {
        Map<Uuid, TopicMetadata> topicMetadata = new HashMap<>();
        topicMetadata.put(topic1Uuid, new TopicMetadata(
            topic1Uuid,
            topic1Name,
            3,
```

**How to spot**
When reviewing group-coordinator/src code, look for missing null / bounds check patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-16930; uniformheterogeneousassignmentbuilder throws npe when one member has no subscriptions (#16283).

**Test**
Test modified in UniformHeterogeneousAssignmentBuilderTest.java covering the corrected behaviour.
