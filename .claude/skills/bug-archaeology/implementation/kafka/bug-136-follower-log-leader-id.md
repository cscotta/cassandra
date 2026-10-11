**Commit**: cedf1c052e · **Ticket**: KAFKA-19776
**Subsystem**: core/src/main/scala/kafka/cluster/Partition.scala
**Category**: Logic error in condition

**Root cause**
In `Partition.makeFollower()`, the fields `leaderReplicaIdOpt` and `leaderEpoch` were updated in place before the log message describing "Previous leader … and previous leader epoch was …" was emitted. Because the log message read from the now-updated fields it reported the new values as if they were the previous values, making partition-state transition logging misleading. The fix captures both values into local variables (`prevLeaderReplicaIdOpt`, `prevLeaderEpoch`) immediately before the mutations.

**Before/After**
```scala
// BEFORE – mutates fields, then logs them as "previous" values
leaderReplicaIdOpt = Option(partitionRegistration.leader)
leaderEpoch = partitionRegistration.leaderEpoch
...
stateChangeLogger.info(s"... Previous leader $leaderReplicaIdOpt and " +
  s"previous leader epoch was $leaderEpoch.")   // logs new values!

// AFTER – snapshot old values first
val prevLeaderReplicaIdOpt = leaderReplicaIdOpt
val prevLeaderEpoch = leaderEpoch
leaderReplicaIdOpt = Option(partitionRegistration.leader)
leaderEpoch = partitionRegistration.leaderEpoch
...
stateChangeLogger.info(s"... Previous leader $prevLeaderReplicaIdOpt and " +
  s"previous leader epoch was $prevLeaderEpoch.")
```

**How to spot**
The pattern of "log old value / assign new value" is easily inverted. Whenever a log message needs to refer to the value of a mutable field before an update, always capture it into a local `val`/`final` variable before the mutation. Code review should flag any use of an instance field in a "previous" log context.

**Test**
No new test added; this is a logging accuracy fix.
