**Commit**: a5c71bd6cb · **Ticket**: KAFKA-16948
**Subsystem**: core/src
**Category**: Resource leak

**Root cause**
When the node transitions from a leader to a follower for a partition, then the tier-lag metrics should be reset to zero. Otherwise, it would lead to false positive in metrics. Addressed the concurrency issue while emitting the metrics. Reviewers: Satish Duggana <satishd@apache.org>, Francois Visconte <f.visconte@gmail.com>,

**Before/After**
```java
// Before
            followerPartitions.forEach(this::removeRemoteTopicPartitionMetrics);
            String topic = topicIdPartition.topic();
            int partition = topicIdPartition.partition();
            brokerTopicStats.recordRemoteCopyLagBytes(topic, partition, bytesLag);
            brokerTopicStats.recordRemoteCopyLagSegments(topic, partition, segmentsLag);

// After
            // If this node was the previous leader for the partition, then the RLMTask might be running in the
            // background thread and might emit metrics. So, removing the metrics after marking this node as follower.
            followerPartitions.forEach(this::removeRemoteTopicPartitionMetrics);

    // VisibleForTesting
    RLMTask rlmTask(TopicIdPartition topicIdPartition) {
        RLMTaskWithFuture task = leaderOrFollowerTasks.get(topicIdPartition);
        if (task != null) {
            return task.rlmTask;
        }
        return null;
    }
```

**How to spot**
When reviewing core/src code, look for resource leak patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-16948: reset tier lag metrics on becoming follower (#16321).

**Test**
Test modified in RemoteLogManagerTest.java covering the corrected behaviour.
