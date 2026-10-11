**Commit**: 9a85705b56 · **Ticket**: KAFKA-17297
**Subsystem**: raft/src
**Category**: State not cleaned up

**Root cause**
In old code leaderId and oldLeaderId all use randomReplicaId(), thus it will be flaky when leaderId is equals oldLeaderId. When KafkaRaftClient initialize it will find leaderId and oldLeaderId, so it will tansfer to candidate, and the leaderEpoch will be + 1

Reviewers: TengYao Chi <kitingiao@gmail.com>, Chia-Ping Tsai <chia7712@gmail.com>

**Before/After**
```java
// Before
        int oldLeaderId = randomReplicaId() + 1;

// After
        int oldLeaderId = localId + 1;
```

**How to spot**
When reviewing raft/src code, look for state not cleaned up patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-17297 stabalize testhandleendquorumrequest  (#16855).

**Test**
Test modified in KafkaRaftClientTest.java covering the corrected behaviour.
