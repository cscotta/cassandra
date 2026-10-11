**Commit**: ee71156295 · **Ticket**: KAFKA-17332
**Subsystem**: core/src
**Category**: Logic error in condition

**Root cause**
This change includes two improvements. When the leader removes itself from the voters set clients of RaftClient may call resign. In those cases the leader is not in the voter set and should not throw an exception. Controllers that are observers must flush the log on every append because leader may be trying to add them to the voter set.

**Before/After**
```java
// Before
        if (quorum.isVoter()) {
            // the leader only requires that voters have flushed their log before sending
            // a Fetch request
        } else if (!quorum.isVoter()) {
            throw new IllegalStateException("Attempt to resign by a non-voter");
        ReplicaKey local = replicaKey(randomeReplicaId(), true);

// After
      // Controllers should always flush the log on replication because they may become voters
      config.processRoles.contains(ProcessRole.ControllerRole),
    private final boolean followersAlwaysFlush;
     *
     * @param followersAlwaysFlush instruct followers to always fsync when appending to the log
        boolean followersAlwaysFlush,
            followersAlwaysFlush,
        boolean followersAlwaysFlush,
        this.followersAlwaysFlush = followersAlwaysFlush;
        if (quorum.isVoter() || followersAlwaysFlush) {
            // the leader only requires that voters have flushed their log before sending a Fetch
            // request. Because of reconfiguration some observers (that are getting added to the
```

**How to spot**
When reviewing core/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-17332; controller always flush and can call resign on observers (#16907).

**Test**
Test modified in KafkaRaftClientReconfigTest.java covering the corrected behaviour.
