**Commit**: 30c511d640 · **Ticket**: KAFKA-19085
**Subsystem**: core/src/test/java/kafka/server/share/SharePartitionManagerTest.java
**Category**: State not cleaned up

**Root cause**
The test `testMultipleConcurrentShareFetches` used incomplete mock setup: `SharePartition` mocks were not configured to answer `maybeInitialize()`, `maybeAcquireFetchLock()`, `canAcquireRecords()`, `acquire()`, or `fetchOffsetMetadata()`. When the `DelayedShareFetch` purgatory processed requests, these unmocked calls returned `null` or `false`, causing the delayed fetch to throw a silent exception (`ERROR Error processing delayed share fetch request`). Additionally, the `FETCH_PARAMS` used a `minBytes=0` value causing immediate completion without waiting, breaking the concurrency scenario. The fix adds all missing mock stubs and injects the share partition cache map directly into the manager builder.

**Before/After**
```java
// Before: no acquire/init mocks, share partitions created after manager
sharePartitionManager = SharePartitionManagerBuilder.builder()
    .withReplicaManager(mockReplicaManager)
    .withTimer(mockTimer)
    .build();
// sp0, sp1, sp2, sp3 created but not injected or properly mocked

// After: all lifecycle mocks set up, partition map injected before manager build
when(sp0.maybeInitialize()).thenReturn(CompletableFuture.completedFuture(null));
when(sp0.maybeAcquireFetchLock()).thenReturn(true);
when(sp0.canAcquireRecords()).thenReturn(true);
when(sp0.acquire(...)).thenReturn(createShareAcquiredRecords(...));
// ...
sharePartitionManager = SharePartitionManagerBuilder.builder()
    .withPartitionCacheMap(partitionCacheMap).build();
```

**How to spot**
When a test mocks a component that participates in an asynchronous pipeline, ensure every method called by the pipeline has a stub. Unmocked Mockito methods return `null` for objects and `false` for booleans — both can silently short-circuit the pipeline without throwing a test-visible assertion failure.

**Test**
This commit repairs the existing `testMultipleConcurrentShareFetches` test so it no longer throws a silent exception and the concurrent fetch scenario actually exercises the intended code path.
