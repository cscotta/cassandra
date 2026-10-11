**Commit**: 9d93a4f68f · **Ticket**: KAFKA-18384
**Subsystem**: core/src
**Category**: Race condition / missing synchronization

**Root cause**
Reviewers: Mickael Maison <mickael.maison@gmail.com>

**Before/After**
```java
// Before
import kafka.zk.KafkaZkClient

  /**
   * Factory for ZK based implementation, used when IBP < 2.7-IV2
   */
  def apply(
    scheduler: Scheduler,
    time: Time,
    zkClient: KafkaZkClient
  ): AlterPartitionManager = {
    new ZkAlterPartitionManager(scheduler, time, zkClient)
  }

// After
// (no additions)
```

**How to spot**
When reviewing core/src code, look for race condition / missing synchronization patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-18384 remove zkalterpartitionmanager (#18364).

**Test**
Test modified in PartitionTest.scala covering the corrected behaviour.
