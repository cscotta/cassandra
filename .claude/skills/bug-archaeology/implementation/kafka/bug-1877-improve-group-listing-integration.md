**Commit**: f2e1a4ca8b · **Ticket**: KAFKA-17548
**Subsystem**: core/src
**Category**: Logic error in condition

**Root cause**
Reviewers: Apoorv Mittal <apoorvmittal10@gmail.com>, Manikumar Reddy <manikumar.reddy@gmail.com>

**Before/After**
```java
// Before
                group.state.get == ConsumerGroupState.STABLE)
                group.state.get == ConsumerGroupState.STABLE)
                group.state.get == ConsumerGroupState.STABLE)
                group.state.get == ConsumerGroupState.STABLE)
    val longTopicName = String.join("", Collections.nCopies(249, "x"));
    val invalidTopicName = String.join("", Collections.nCopies(250, "x"));
    val anc

// After
                group.state.get == ConsumerGroupState.STABLE &&
                group.groupState.get == GroupState.STABLE)
                group.state.get == ConsumerGroupState.STABLE &&
                group.groupState.get == GroupState.STABLE)
                group.state.get == ConsumerGroupState.STABLE &&
                group.groupState.get == GroupState.STABLE)
            matching.size == 1
          }, s"Expected to be able to list $testGroupId in group type $groupType and state Stable")

          TestUtils.waitUntilTrue(() => {
            val options = new ListConsumerGroupsOptions().withTypes(Set(groupType).asJava)
              .inGroupStates(Set(GroupState.STABLE).asJava)
```

**How to spot**
When reviewing core/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-17548: improve group listing integration test (#18081).

**Test**
Test modified in PlaintextAdminIntegrationTest.scala covering the corrected behaviour.
