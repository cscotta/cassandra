**Commit**: 12a60b8cd9 · **Ticket**: KAFKA-17878
**Subsystem**: core/src
**Category**: Logic error in condition

**Root cause**
Reviewers: Chia-Ping Tsai <chia7712@gmail.com>

**Before/After**
```java
// Before
import kafka.server.DelayedActionQueue;
            replicaManager.addToActionQueue(() -> {
                topicPartitionData.keySet().forEach(topicIdPartition ->
                    replicaManager.completeDelayedShareFetchRequest(
                        new DelayedShareFetchGroupKey(shareFetchData.groupId(), topicIdPartition.topicId(), topicIdPartition.partition())));
                return BoxedUnit.UNIT;
            });
import kafka.server.{ActionQueue, ReplicaManager, defaultError, genericError}
    override def add(action: () => Unit): Unit = {
      action()
/**
  * Licensed to the Apache Software Foundation (ASF) under one or more

// After
import org.apache.kafka.server.DelayedActionQueue;
            replicaManager.addToActionQueue(() -> topicPartitionData.keySet().forEach(topicIdPartition ->
                replicaManager.completeDelayedShareFetchRequest(
                    new DelayedShareFetchGroupKey(shareFetchData.groupId(), topicIdPartition.topicId(), topicIdPartition.partition()))));
import kafka.server.{ReplicaManager, defaultError, genericError}
import org.apache.kafka.server.ActionQueue
    override def add(action: Runnable): Unit = {
      action.run()
```

**How to spot**
When reviewing core/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-17878 move actionqueue to server module (#17602).

**Test**
Test modified in AbstractCoordinatorConcurrencyTest.scala covering the corrected behaviour.
