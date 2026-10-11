**Commit**: 16186eabcd · **Ticket**: KAFKA-16927
**Subsystem**: raft/src
**Category**: State not cleaned up

**Root cause**
When a replica restarts in the follower state it is possible for the set of leader endpoints to not match the latest set of leader endpoints. Voters will discover the latest set of leader endpoints through the BEGIN_QUORUM_EPOCH request. This means that KRaft needs to allow for the replica to transition from Follower to Follower when only the set of leader endpoints has changed. Reviewers: Colin P.

**Before/After**
```java
// Before
        if (localId.isPresent() && leaderId == localId.getAsInt()) {
            throw new IllegalStateException("Cannot transition to Follower with leader " + leaderId +
                " and epoch " + epoch + " since it matches the local broker.id " + localId);
            throw new IllegalStateException("Cannot transition to Follower with leader " + leaderId +
                " and epoch " + epoch + " since the current epoch " + currentEpoch + " is larger");
        } else if (epoch == currentEpoch && (isFollower() || isLeader())) {
            throw new IllegalStateException("Cannot transition to Follower with leader " + leaderId +
                " and epoch " + epoch + " from state " + state);

// After
        if (endpoints.isEmpty()) {
            throw new IllegalArgumentException(
                String.format(
                    "Cannot transition to Follower with leader %s and epoch %s without a leader endpoint",
                    leaderId,
                    epoch
                )
            );
        } else if (localId.isPresent() && leaderId == localId.getAsInt()) {
            throw new IllegalStateException(
                String.format(
                    "Cannot transition to Follower with leader %s and epoch %s since it matches the local node.id %s",
```

**How to spot**
When reviewing raft/src code, look for state not cleaned up patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-16927; handle expanding leader endpoints (#17363).

**Test**
Test modified in KafkaRaftClientReconfigTest.java covering the corrected behaviour.
