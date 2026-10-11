**Commit**: 5e23df0c8d · **Ticket**: KAFKA-18486
**Subsystem**: core/src/test/scala/unit/kafka/server/ReplicaManagerTest.scala
**Category**: State not cleaned up

**Root cause**
Several `ReplicaManagerTest` test methods used the deprecated `becomeLeaderOrFollower()` API (which relies on the legacy ZooKeeper-style `LeaderAndIsrRequest` path) to set up partition state. In newer KRaft-based code this path is no longer exercised and the test scaffolding left partition state that could interfere with subsequent assertions. The migration to `applyDelta()` (the KRaft metadata-image path) correctly initialises partition state and also removes dead code such as the unused `correlationId` constant and the manual `createPartition`/`createLogIfNotExists` calls.

**Before/After**
```scala
// BEFORE – uses deprecated ZK-style setup
val partition = rm.createPartition(new TopicPartition(topic, 0))
partition.createLogIfNotExists(isNew = false, ...)
rm.becomeLeaderOrFollower(0, new LeaderAndIsrRequest.Builder(...).build(), ...)

// AFTER – uses KRaft-native setup
val delta = topicsCreateDelta(0, isStartIdLeader = true, ...)
val image = imageFromTopics(delta.apply())
rm.applyDelta(delta, image)
val partition = rm.getPartitionOrException(topicPartition)
```

**How to spot**
Tests calling `becomeLeaderOrFollower` in a KRaft-mode `ReplicaManager` test class are using a deprecated code path that no longer matches production behaviour. Any test that manually calls `createPartition` + `createLogIfNotExists` + `becomeLeaderOrFollower` should be migrated to `applyDelta`.

**Test**
This commit is itself a test migration. The updated tests (`testInconsistentIdReturnsError`, `testMaybeAddLogDirFetchers`, etc.) continue to assert the same functional outcomes but through the correct KRaft API.
