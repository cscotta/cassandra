**Commit**: ce1b079884 · **Ticket**: KAFKA-18542
**Subsystem**: core/src
**Category**: Race condition / missing synchronization

**Root cause**
Reviewers: Chia-Ping Tsai <chia7712@gmail.com>

**Before/After**
```java
// Before
import kafka.controller.{KafkaController, StateChangeLogger}
  /* Epoch of the controller that last changed the leader. This needs to be initialized correctly upon broker startup.
   * One way of doing that is through the controller's start replica state change command. When a new broker starts up
   * the controller sends it a start replica command containing the leader for each partition that the broker hosts.
   * In addition to the leader, the controller can also send the epoch of the controller that elected the leader for
   * each partition. */
  private var controllerEpoch: Int = KafkaController.InitialControllerEpoch
      // Record the epoch of the controller that made the leadership decision. This is useful while updating the isr
      // to maintain the decision maker controller's epoch in the zookeeper path.
      controllerEpoch = partitionState.controllerEpoch

      // Record the epoch of the controller that made the leadership decision. This is useful while updating the isr

// After
import kafka.controller.StateChangeLogger
      proposedIsrState.sentLeaderAndIsr
 * Handles updating the ISR by sending AlterPartition requests to the controller. Updating the ISR is an asynchronous
 * operation, so partitions will learn about the result of their request through a callback.
    leaderAndIsr: LeaderAndIsr
  future: CompletableFuture[LeaderAndIsr]
   * Factory to AlterPartition based implementation
```

**How to spot**
When reviewing core/src code, look for race condition / missing synchronization patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-18542 cleanup alterpartitionmanager (#18552).

**Test**
Test modified in PartitionLockTest.scala covering the corrected behaviour.
