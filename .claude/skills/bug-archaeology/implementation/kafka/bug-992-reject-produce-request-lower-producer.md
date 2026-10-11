**Commit**: cc25d217da · **Ticket**: KAFKA-18042
**Subsystem**: storage/src
**Category**: State not cleaned up

**Root cause**
With the transaction V2, replica manager checks whether the incoming
producer request produces to a partition belonging to a transaction. ReplicaManager figures this out by checking the producer epoch stored in
the partition log. However, the current code does not reject the produce
request if its producer epoch is lower than the stored producer epoch. It is an optimization to reject such requests earlier instead of sending
an AddPartitionToTxn request and getting rejected in the response.

**Before/After**
```java
// Before
// (no removals)

// After
  @Test
  def testTransactionVerificationRejectsLowerProducerEpoch(): Unit = {
    val tp0               = new TopicPartition(topic, 0)
    val producerId        = 24L
    val producerEpoch     = 5.toShort
    val lowerProducerEpoch= 4.toShort
    val sequence          = 6
    val addPartitionsToTxnManager = mock(classOf[AddPartitionsToTxnManager])

    val replicaManager =
      setUpReplicaManagerWithMockedAddPartitionsToTxnManager(addPartitionsToTxnManager, List(tp0))

```

**How to spot**
When reviewing storage/src code, look for state not cleaned up patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-18042: reject the produce request with lower producer epoch early (kip-890) (#19844).

**Test**
Test modified in ReplicaManagerTest.scala covering the corrected behaviour.
