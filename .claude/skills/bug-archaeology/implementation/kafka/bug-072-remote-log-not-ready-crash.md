**Commit**: 93adaea599 · **Ticket**: KAFKA-19523
**Subsystem**: core/src/main/java/kafka/server/TierStateMachine.java
**Category**: Missing null / bounds check

**Root cause**
When a follower with an empty disk joined a cluster and began replicating a topic with remote storage enabled, `ReplicaFetcherThread` called `TierStateMachine.buildRemoteLogAuxState()` to reconstruct the remote-log auxiliary state. If the `RemoteLogManager` had not yet finished initializing for that partition (because `makeFollowers` ran before `RemoteLogManager#onLeadershipChange`), `fetchRemoteLogSegmentMetadata` threw an untyped `Exception` that propagated up as an unhandled error and terminated the fetcher thread permanently. The fix adds an explicit readiness check and throws a new `RemoteStorageNotReadyException` (a subclass of `RetriableRemoteStorageException`), which the fetcher thread already handles gracefully by logging a warning and retrying.

**Before/After**
```java
// Before: no readiness check; fetchRemoteLogSegmentMetadata throws unexpectedly
RemoteLogSegmentMetadata meta = rlm.fetchRemoteLogSegmentMetadata(...)
        .orElseThrow(() -> buildRemoteStorageException(...));

// After: guard before the call
if (!rlm.isPartitionReady(topicPartition)) {
    throw new RemoteStorageNotReadyException(
        "RemoteLogManager is not ready for partition: " + topicPartition);
}
RemoteLogSegmentMetadata meta = rlm.fetchRemoteLogSegmentMetadata(...);
```

**How to spot**
Anywhere an asynchronous component (like `RemoteLogManager`) is called during startup, verify that the component's readiness is checked first. The typical pattern is an `isReady()` or `isPartitionReady()` guard. When the failure mode of a missing readiness check produces a non-retriable exception that terminates a fetcher or background thread, the system can stall indefinitely.

**Test**
`ReplicaManagerTest` was updated to mock `remoteLogManager.isPartitionReady(any())` returning `true` for the existing test, and a comment clarifies that the test now expects `failedBuildRemoteLogAuxStateRate` to increment because the RLM is not ready (rather than because metadata is absent).
