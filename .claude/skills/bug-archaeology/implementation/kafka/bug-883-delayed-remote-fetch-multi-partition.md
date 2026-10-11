**Commit**: e1ff387605 · **Ticket**: KAFKA-14915

**Subsystem**: Remote Storage / DelayedRemoteFetch

**Category**: Logic error in condition

**Root cause**
`DelayedRemoteFetch` was designed to handle a single remote partition per delayed fetch operation. When a fetch request spanned multiple partitions stored in remote storage, only the first remote partition's fetch was tracked and completed; the rest were silently ignored, producing incomplete fetch results. The fix refactors `DelayedRemoteFetch` to hold maps from `TopicIdPartition` to `RemoteStorageFetchInfo`, `RemoteFetchTask`, and result, enabling it to manage multiple remote partitions concurrently and complete all of them before satisfying the delayed operation.

**Before/After**
```java
// Before: single remote partition per delayed fetch
private final RemoteStorageFetchInfo remoteFetchInfo;
private volatile RemoteFetchTask remoteFetchTask;

// After: maps keyed by TopicIdPartition for multiple partitions
private final Map<TopicIdPartition, RemoteStorageFetchInfo> remoteFetchInfos;
private final Map<TopicIdPartition, RemoteFetchTask> remoteFetchTasks;
private final Map<TopicIdPartition, RemoteFetchResult> remoteFetchResults;
```

**How to spot**
A delayed operation class with a single scalar field for a per-partition resource (task, future, result) will silently drop all but one partition when a multi-partition request is processed. Always use a collection keyed by partition identity.

**Test**
`DelayedRemoteFetchTest` adds multi-partition scenarios and verifies that results for all remote partitions are included in the completed fetch response.
