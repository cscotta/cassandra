**Commit**: 90087e416b · **Ticket**: KAFKA-19776
**Subsystem**: core/src/main/scala/kafka/cluster/Partition.scala
**Category**: Logic error in condition

**Root cause**
In `Partition.makeFollower()`, after updating `leaderEpoch` and `leaderReplicaIdOpt` in place, the "skipped state change" log message referenced the already-updated field `leaderEpoch` instead of capturing the previous value. The log therefore printed the new epoch in the "already a follower with leader epoch X" message, making it impossible to tell from logs what the previous epoch was. A companion commit (cedf1c052e) had already fixed the "previous leader" log message in the successful-transition branch; this commit fixes the remaining branch.

**Before/After**
```scala
// BEFORE – leaderEpoch already updated when log message runs
stateChangeLogger.info(s"Skipped the become-follower state change for $topicPartition ... " +
  s"since it is already a follower with leader epoch $leaderEpoch.")

// AFTER – prevLeaderEpoch captured before the field was mutated
stateChangeLogger.info(s"Skipped the become-follower state change for $topicPartition ... " +
  s"since it is already a follower with leader epoch $prevLeaderEpoch.")
```

**How to spot**
Any log or diagnostic message that describes "the previous value of X" but is emitted after X is mutated will silently log the wrong value. Grep for field reads inside log strings that occur after the same field is assigned earlier in the same method block.

**Test**
No new test added; the change is a logging correctness fix only.
