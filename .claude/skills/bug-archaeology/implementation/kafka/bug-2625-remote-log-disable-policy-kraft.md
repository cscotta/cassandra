**Commit**: 9f7e8d478a · **Ticket**: KAFKA-16855
**Subsystem**: clients/src
**Category**: Logic error in condition

**Root cause**
Reviewers: Kamal Chandraprakash <kamal.chandraprakash@gmail.com>, Christo Lolov <lolovc@amazon.com>

**Before/After**
```java
// Before
    public static final String REMOTE_LOG_DISABLE_POLICY_RETAIN = "retain";
    public static final String REMOTE_LOG_DISABLE_POLICY_DELETE = "delete";

    public static final String REMOTE_LOG_DISABLE_POLICY_CONFIG = "remote.log.disable.policy";

    public static final String REMOTE_LOG_DISABLE_POLICY_DOC = String.format("Determines whether tiered data for a topic should be retained or " +
            "deleted after tiered storage disablement on a topic. The two valid options are \"%s\" and \"%s\". If %s is " +
            "selected then all data in remote will be kept post-disablement and will only be deleted when it breaches expiration " +
            "thresholds. If %s is selected then the data will be made inaccessible immediately by advancing the log start offset and will be " +
            "deleted asynchronously.", REMOTE_LOG_DISABLE_POLICY_RETAIN, REMOTE_LOG_DISABLE_POLICY_DELETE,
            REMOTE_LOG_DISABLE_POLICY_RETAIN, REMOTE_LOG_DISABLE_POLICY_DELETE);
        Set<TopicIdPartition> leaderPartitions = filterPartitions(partitionsBecomeLeader)

// After
    public static final String REMOTE_LOG_COPY_DISABLE_CONFIG = "remote.log.copy.disable";
    public static final String REMOTE_LOG_COPY_DISABLE_DOC = "Determines whether tiered data for a topic should become read only," +
            " and no more data uploading on a topic.";

    public static final String REMOTE_LOG_DELETE_ON_DISABLE_CONFIG = "remote.log.delete.on.disable";
    public static final String REMOTE_LOG_DELETE_ON_DISABLE_DOC = "Determines whether tiered data for a topic should be " +
            "deleted after tiered storage is disabled on a topic. This configuration should be enabled when trying to " +
            "set `remote.storage.enable` from true to false";
        Map<TopicIdPartition, Boolean> leaderPartitions = filterPartitions(partitionsBecomeLeader)
                .collect(Collectors.toMap(p -> new TopicIdPartition(topicIds.get(p.topic()), p.topicPartition()),
                        p -> p.log().exists(log -> log.config().remoteLogCopyDisable())));
        Map<TopicIdPartition, Boolean> followerPartitions = filterPartitions(partitionsBecomeFollower)
```

**How to spot**
When reviewing clients/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-16855: remote log disable policy in kraft (#16653).

**Test**
Test modified in RemoteLogManagerTest.java covering the corrected behaviour.
