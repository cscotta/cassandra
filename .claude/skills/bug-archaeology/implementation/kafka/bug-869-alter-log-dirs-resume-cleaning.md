**Commit**: 12761c07ae · **Ticket**: KAFKA-19458

**Subsystem**: Replication / ReplicaManager

**Category**: Missing error handler / unimplemented code path

**Root cause**
`ReplicaManager.alterReplicaLogDirs` handles requests to move a partition log to a different directory. When the destination directory was changed while a future replica was already in progress, the code removed the future replica but failed to call `logManager.resumeCleaning()`. This left the log cleaner suspended indefinitely for that partition, preventing compaction from running until a broker restart.

**Before/After**
```java
// Before: future replica removed but cleaner not resumed
removeFutureLocalReplica(topicPartition);

// After: cleaner resumed after removing future replica
removeFutureLocalReplica(topicPartition);
logManager.resumeCleaning(topicPartition);
```

**How to spot**
Whenever `removeFutureLocalReplica` or similar abort-future-replica operations are performed, check whether a corresponding `resumeCleaning` call exists; the cleaner is suspended when future log copying begins and must be explicitly resumed if the copy is cancelled.

**Test**
`ReplicaManagerTest` adds a scenario where `alterReplicaLogDirs` is called twice for the same partition (destination change mid-copy) and asserts that `logManager.resumeCleaning` is invoked for the partition.
