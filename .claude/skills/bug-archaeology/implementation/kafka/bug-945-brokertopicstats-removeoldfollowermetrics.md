**Commit**: 79d2c3c62a · **Ticket**: KAFKA-19406
**Subsystem**: core/src
**Category**: Logic error in condition

**Root cause**
BTW: whether we should rename
`ReplicaManager#updateLeaderAndFollowerMetrics`

Reviewers: Ken Huang <s7133700@gmail.com>, PoAn Yang
 <payang@apache.org>, TengYao Chi <kitingiao@gmail.com>, Lan Ding
 <isDing_L@163.com>, Chia-Ping Tsai <chia7712@gmail.com>

**Before/After**
```java
// Before

    // remove metrics for brokers which are not followers of a topic
    leaderTopicSet.diff(newFollowerTopics).foreach(brokerTopicStats.removeOldFollowerMetrics)
            topicMetrics.closeMetric(BrokerTopicMetrics.REPLICATION_BYTES_OUT_PER_SEC);
            topicMetrics.closeMetric(BrokerTopicMetrics.REASSIGNMENT_BYTES_OUT_PER_SEC);
    // This method only removes metrics only used for follower
    public void removeOldFollowerMetrics(String topic) {
        BrokerTopicMetrics topicMetrics = topicStats(topic);
        if (topicMetrics != null) {
            topicMetrics.closeMetric(BrokerTopicMetrics.REPLICATION_BYTES_IN_PER_SEC);
            topicMetrics.closeMetric(BrokerTopicMetrics.REASSIGNMENT_BYTES_IN_PER_SEC);
        }

// After
    // Currently, there are no follower metrics that need to be updated.
```

**How to spot**
When reviewing core/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-19406 remove brokertopicstats#removeoldfollowermetrics (#19962).

**Test**
No test added.
