**Commit**: dbae448a05 · **Ticket**: KAFKA-18137
**Subsystem**: core/src
**Category**: State not cleaned up

**Root cause**
When there is a become follower transition on a transaction coordinator state partition, we intend to unload the state partition. However, we pass the new epoch to the method that does the unloading. In that method, we create a `TransactionPartitionAndLeaderEpoch` object comprising of the topic partition and the epoch that we use as a key to remove the partition from loading. However, we wouldn't ever expect to see this epoch in that map since we only load on the leader.

**Before/After**
```java
// Before
    val partitionAndLeaderEpoch = TransactionPartitionAndLeaderEpoch(partitionId, coordinatorEpoch)
      loadingPartitions.remove(partitionAndLeaderEpoch)

// After
      removeLoadingPartitionWithEpoch(partitionId, coordinatorEpoch)
  /**
   * Remove the loading partition if the epoch is less than the specified epoch. Note: This method must be called under the write state lock.
   */
  private def removeLoadingPartitionWithEpoch(partitionId: Int, coordinatorEpoch: Int): Unit = {
    loadingPartitions.find(_.txnPartitionId == partitionId).foreach { partitionAndLeaderEpoch =>
      if (partitionAndLeaderEpoch.coordinatorEpoch < coordinatorEpoch) {
        loadingPartitions.remove(partitionAndLeaderEpoch)
        info(s"Cancelling load of currently loading partition $partitionAndLeaderEpoch")
      }
    }
  }
```

**How to spot**
When reviewing core/src code, look for state not cleaned up patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-18137: unloading transaction state incorrectly removes loading partitions (#18011).

**Test**
Test modified in TransactionStateManagerTest.scala covering the corrected behaviour.
