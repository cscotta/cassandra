**Commit**: aae42ef656 · **Ticket**: KAFKA-17593
**Subsystem**: clients/src
**Category**: Logic error in condition

**Root cause**
Reviewers: Lianet Magrans <lmagrans@confluent.io>

**Before/After**
```java
// Before
  "latestVersionUnstable": true,
      new ConsumerGroupHeartbeatRequestData(),
      true
      new ConsumerGroupHeartbeatRequestData(),
      true
          .setTopicPartitions(List.empty.asJava),
        true
          .setMemberEpoch(consumerGroupHeartbeatResponse.data.memberEpoch),
        true
          .setMemberEpoch(-1),
        true
          .setTopicPartitions(List.empty.asJava),

// After
      new ConsumerGroupHeartbeatRequestData()
      new ConsumerGroupHeartbeatRequestData()
          .setTopicPartitions(List.empty.asJava)
          .setMemberEpoch(consumerGroupHeartbeatResponse.data.memberEpoch)
          .setMemberEpoch(-1)
          .setTopicPartitions(List.empty.asJava)
          .setMemberEpoch(consumerGroupHeartbeatResponse.data.memberEpoch)
          .setTopicPartitions(List.empty.asJava)
          .setTopicPartitions(List.empty.asJava)
          .setMemberEpoch(consumerGroupHeartbeatResponse.data.memberEpoch)
```

**How to spot**
When reviewing clients/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-17593; [9/n] mark consumergroupheartbeat api v1 as stable (#17961).

**Test**
Test modified in ConsumerGroupHeartbeatRequestTest.scala covering the corrected behaviour.
