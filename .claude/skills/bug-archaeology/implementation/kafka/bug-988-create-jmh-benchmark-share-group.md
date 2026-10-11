**Commit**: 5cf8b2abb0 · **Ticket**: KAFKA-19370
**Subsystem**: jmh-benchmarks/src
**Category**: Logic error in condition

**Root cause**
As part of readying share groups for production, we want to ensure that
the performance of the server-side assignor is optimal. In common with
consumer group assignors, a JMH benchmark is used for the analysis. Reviewers: Apoorv Mittal <apoorvmittal10@gmail.com>

**Before/After**
```java
// Before
     * Creates a GroupSpec from the given ConsumerGroupMembers.
     * @param members               The ConsumerGroupMembers.
     * @param subscriptionType      The group's subscription type.
        Map<String, ConsumerGroupMember> members,
        for (Map.Entry<String, ConsumerGroupMember> memberEntry : members.entrySet()) {
            ConsumerGroupMember member = memberEntry.getValue();

// After
import org.apache.kafka.coordinator.group.modern.ModernGroupMember;
import org.apache.kafka.coordinator.group.modern.share.ShareGroupMember;
     * Creates a GroupSpec from the given ModernGroupMembers.
     * @param members               The ModernGroupMembers.
     * @param subscriptionType      The group's subscription type.
        Map<String, ? extends ModernGroupMember> members,
        for (Map.Entry<String, ? extends ModernGroupMember> memberEntry : members.entrySet()) {
            ModernGroupMember member = memberEntry.getValue();
    /**
     * Creates a ShareGroupMembers map where all members have the same topic subscriptions.
     *
     * @param memberCount           The number of members in the group.
```

**How to spot**
When reviewing jmh-benchmarks/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-19370: create jmh benchmark for share group assignor (#19907).

**Test**
No test added.
