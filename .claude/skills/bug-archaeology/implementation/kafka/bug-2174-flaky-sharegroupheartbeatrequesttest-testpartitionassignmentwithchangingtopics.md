**Commit**: 2267518d5b · **Ticket**: KAFKA-17407
**Subsystem**: core/src
**Category**: Logic error in condition

**Root cause**
Reviewers: Andrew Schofield <aschofield@confluent.io>, Chia-Ping Tsai <chia7712@gmail.com>

**Before/After**
```java
// Before
    // to an nonexistent topic.
    assertEquals(3, shareGroupHeartbeatResponse.data.memberEpoch)
        .setMemberEpoch(3),
    assertEquals(4, shareGroupHeartbeatResponse.data.memberEpoch)
        .setMemberEpoch(4),
    assertEquals(5, shareGroupHeartbeatResponse.data.memberEpoch)
        .setMemberEpoch(5),
    assertEquals(6, shareGroupHeartbeatResponse.data.memberEpoch)

// After
    // to a nonexistent topic.
    cluster.waitForTopic("foo", 2)
    cluster.waitForTopic("bar", 3)

    assertEquals(2, shareGroupHeartbeatResponse.data.memberEpoch)
        .setMemberEpoch(2),
    assertEquals(3, shareGroupHeartbeatResponse.data.memberEpoch)
        .setMemberEpoch(3),
    assertEquals(4, shareGroupHeartbeatResponse.data.memberEpoch)
        .setMemberEpoch(4),
    assertEquals(5, shareGroupHeartbeatResponse.data.memberEpoch)
```

**How to spot**
When reviewing core/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-17407 fix flaky sharegroupheartbeatrequesttest.testpartitionassignmentwithchangingtopics (#16994).

**Test**
Test modified in ShareGroupHeartbeatRequestTest.scala covering the corrected behaviour.
