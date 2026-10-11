**Commit**: 7293f3a90e · **Ticket**: KAFKA-19183
**Subsystem**: checkstyle/import-control-jmh-benchmarks.xml
**Category**: Race condition / missing synchronization

**Root cause**
1. Replace `Pool.scala` with `ConcurrentHashMap`. 2. Remove `PoolTest.scala`.

**Before/After**
```java
// Before
    <allow class="kafka.utils.Pool"/>
import java.util.concurrent.{CompletableFuture, CopyOnWriteArrayList}
  private val remoteReplicasMap = new Pool[Int, Replica]
    remoteReplicasMap.values
      val removedReplicas = remoteReplicasMap.keys.filterNot(followers.contains(_))
      followers.foreach(id => remoteReplicasMap.getAndMaybePut(id, new Replica(id, topicPartition, metadataCache)))
      remoteReplicasMap.removeAll(removedReplicas)
    remoteReplicasMap.values.foreach { replica =>
import kafka.utils.{Logging, Pool}
        entry.metadataPerTransactionalId.filter { case (_, txnMetadata) =>
          val stateEntries = txnMetadataCacheEntry.metadataPerTransactionalId.values.iterator.buffered
          cache.metadata

// After
import java.util.concurrent.{CompletableFuture, ConcurrentHashMap, CopyOnWriteArrayList}
  private val remoteReplicasMap = new ConcurrentHashMap[Int, Replica]
    remoteReplicasMap.values.asScala
      followers.foreach(id => remoteReplicasMap.computeIfAbsent(id, _ => new Replica(id, topicPartition, metadataCache)))
      remoteReplicasMap.keySet.removeIf(replica => !followers.contains(replica))
    remoteReplicasMap.forEach { (_, replica) =>
import java.util.concurrent.{ConcurrentHashMap, ConcurrentMap}
import kafka.utils.Logging
        entry.metadataPerTransactionalId.asScala.filter { case (_, txnMetadata) =>
          val stateEntries = txnMetadataCacheEntry.metadataPerTransactionalId.values.asScala.iterator.buffered
```

**How to spot**
When reviewing checkstyle/import-control-jmh-benchmarks.xml code, look for race condition / missing synchronization patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-19183 replace pool with concurrenthashmap (#19535).

**Test**
Test modified in TransactionCoordinatorConcurrencyTest.scala covering the corrected behaviour.
