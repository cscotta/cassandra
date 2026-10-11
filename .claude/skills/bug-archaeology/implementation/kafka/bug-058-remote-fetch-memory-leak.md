**Commit**: c58cf1df9f · **Ticket**: KAFKA-19763
**Subsystem**: core/src/main/scala/kafka/server/ReplicaManager.scala
**Category**: Resource leak

**Root cause**
When a single FETCH request triggered multiple simultaneous remote reads (one per partition), each produced a `DelayedRemoteFetch` that held a `RemoteLogReadResult` — up to `fetch.max.bytes` (50 MB) in memory. The `DelayedOperationPurgatory` for `RemoteFetch` used the default purge interval, so completed operations were not promptly evicted from the purgatory's watched list. Additionally, the keys used for the purgatory watch were all partitions in the fetch request, including those served locally, so the purgatory watched more keys than were actually awaited, preventing timely cleanup. Together these issues meant completed `DelayedRemoteFetch` objects (and their large buffers) accumulated, causing OOM.

**Before/After**
```scala
// Before: default purge interval; all fetch partitions used as keys
val delayedRemoteFetchPurgatory = new DelayedOperationPurgatory[DelayedRemoteFetch]("RemoteFetch", config.brokerId)
val delayedFetchKeys = remoteFetchPartitionStatus.map { case (tp, _) => new TopicPartitionOperationKey(tp) }

// After: purge interval = 0 for immediate GC; only remote-fetch tasks used as keys
val delayedRemoteFetchPurgatory = new DelayedOperationPurgatory[DelayedRemoteFetch]("RemoteFetch", config.brokerId, 0)
val delayedFetchKeys = remoteFetchTasks.asScala.map { case (tp, _) => new TopicPartitionOperationKey(tp) }
```

**How to spot**
Any purgatory holding large objects (fetch results, bulk data) should use a purge interval of 0 or near-zero so completed operations are released for GC promptly. Review all `DelayedOperationPurgatory` instantiations and check whether the objects they hold are heap-significant. Also verify that purgatory watch keys exactly match the operations being awaited, not a superset.

**Test**
`testMultipleRemoteFetchCountInOneFetchRequestWhenBreachingFetchMaxBytes` verifies that after remote reads complete, `delayedRemoteFetchPurgatory.watched == 0`, confirming no completed operations linger.
