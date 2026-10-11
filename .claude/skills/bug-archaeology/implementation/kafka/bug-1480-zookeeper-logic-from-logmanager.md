**Commit**: 272d947f96 · **Ticket**: KAFKA-18545
**Subsystem**: core/src
**Category**: Logic error in condition

**Root cause**
Reviewers: Chia-Ping Tsai <chia7712@gmail.com>, Ismael Juma <ismael@juma.me.uk>, Mickael Maison <mickael.maison@gmail.com>

**Before/After**
```java
// Before
      // Unlike Zookeeper mode, which tracks pending topic deletions under a ZNode, KRaft is unable to prevent a topic from being recreated before every replica has been deleted.
      // A KRaft broker with an offline directory may be unable to detect it still holds a to-be-deleted replica,
    // Combine the default properties with the overrides in zk to create the new LogConfig
      // When running a ZK controller, we may get a log that does not have a topic ID. Assign it here.
      if (log.topicId.isEmpty) {
        topicId.foreach(log.assignTopicId)
      }

  @Test
  def testPartitionMetadataFileCreatedWithExistingLog(): Unit = {
    val replicaManager = setupReplicaManagerWithMockedPurgatories(new MockTimer(time))
    try {

// After
      // We are unable to prevent a topic from being recreated before every replica has been deleted.
      // Broker with an offline directory may be unable to detect it still holds a to-be-deleted replica,

```

**How to spot**
When reviewing core/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-18545: remove zookeeper logic from logmanager (#18592).

**Test**
Test modified in ReplicaManagerTest.scala covering the corrected behaviour.
