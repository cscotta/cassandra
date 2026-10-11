**Commit**: 24ed31739e · **Ticket**: KAFKA-16853
**Subsystem**: checkstyle/suppressions.xml
**Category**: Logic error in condition

**Root cause**
As part of KIP-950, we want to split the RemoteLogManagerScheduledThreadPool into separate thread pools (one for copy and another for expiration). In this change, we are splitting it into three thread pools (one for copy, one for expiration, and another one for follower). We are reusing the same thread pool configuration for all three thread pools. We can introduce new user-facing configurations later.

**Before/After**
```java
// Before
    private final RLMScheduledThreadPool rlmScheduledThreadPool;
    private final ConcurrentHashMap<TopicIdPartition, RLMTaskWithFuture> leaderOrFollowerTasks = new ConcurrentHashMap<>();
        rlmScheduledThreadPool = new RLMScheduledThreadPool(rlmConfig.remoteLogManagerThreadPoolSize());
                return rlmScheduledThreadPool.getIdlePercent();
        Map<TopicIdPartition, Integer> leaderPartitionsWithLeaderEpoch = filterPartitions(partitionsBecomeLeader)
                .collect(Collectors.toMap(
                        partition -> new TopicIdPartition(topicIds.get(partition.topic()), partition.topicPartition()),
                        Partition::getLeaderEpoch));
        Set<TopicIdPartition> leaderPartitions = leaderPartitionsWithLeaderEpoch.keySet();
            followerPartitions.forEach(topicIdPartition ->
                    doHandleLeaderOrFollowerPartitions(topicIdPartition, RLMTask::convertToFollower));

// After
    <suppress checks="MethodLength" files="RemoteLogManager.java"/>
    private final RLMScheduledThreadPool rlmCopyThreadPool;
    private final RLMScheduledThreadPool rlmExpirationThreadPool;
    private final RLMScheduledThreadPool followerThreadPool;
    private final ConcurrentHashMap<TopicIdPartition, RLMTaskWithFuture> leaderCopyRLMTasks = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<TopicIdPartition, RLMTaskWithFuture> leaderExpirationRLMTasks = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<TopicIdPartition, RLMTaskWithFuture> followerRLMTasks = new ConcurrentHashMap<>();
    private final Set<RemoteLogSegmentId> segmentIdsBeingCopied = ConcurrentHashMap.newKeySet();
        rlmCopyThreadPool = new RLMScheduledThreadPool(rlmConfig.remoteLogManagerCopierThreadPoolSize(),
            "RLMCopyThreadPool", "kafka-rlm-copy-thread-pool-");
        rlmExpirationThreadPool = new RLMScheduledThreadPool(rlmConfig.remoteLogManagerExpirationThreadPoolSize(),
            "RLMExpirationThreadPool", "kafka-rlm-expiration-thread-pool-");
```

**How to spot**
When reviewing checkstyle/suppressions.xml code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-16853: split remotelogmanagerscheduledthreadpool (#16502).

**Test**
Test modified in RemoteLogManagerTest.java covering the corrected behaviour.
