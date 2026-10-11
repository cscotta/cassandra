**Commit**: c797f85de4 · **Ticket**: KAFKA-19642
**Subsystem**: core/src
**Category**: Resource leak

**Root cause**
- **Changes**: Replace misused dynamicPerBrokerConfigs with
dynamicDefaultConfigs
- **Reasons**: KRaft servers don't handle the cluser-level configs in
starting

from: https://github.com/apache/kafka/pull/18949/files#r2296809389

Reviewers: Jun Rao <junrao@gmail.com>, Jhen-Yung Hsu
<jhenyunghsu@gmail.com>, PoAn Yang <payang@apache.org>, Chia-Ping Tsai
<chia7712@gmail.com>

---------

Co-authored-by: PoAn Yang <payang@apache.org>

**Before/After**
```java
// Before
          configHandler.processConfigChanges("", dynamicPerBrokerConfigs)

// After
          configHandler.processConfigChanges("", dynamicDefaultConfigs)
    TestNumReplicaFetcherMetricsReporter.testReporters.clear()

    props.put(MetricConfigs.METRIC_REPORTER_CLASSES_CONFIG, classOf[TestNumReplicaFetcherMetricsReporter].getName)
    props.put(ReplicationConfigs.NUM_REPLICA_FETCHERS_CONFIG, "1")
    // Add num.replica.fetchers to the cluster-level config.
    val clusterLevelProps = new Properties
    clusterLevelProps.put(ReplicationConfigs.NUM_REPLICA_FETCHERS_CONFIG, "2")
    reconfigureServers(clusterLevelProps, perBrokerConfig = false, (ReplicationConfigs.NUM_REPLICA_FETCHERS_CONFIG, "2"))

    // Wait for the metrics reporter to be configured
    val initialReporter = TestNumReplicaFetcherMetricsReporter.waitForReporters(1).head
```

**How to spot**
When reviewing core/src code, look for resource leak patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-19642 replace dynamicperbrokerconfigs with dynamicdefaultconfigs (#20405).

**Test**
Test modified in DynamicBrokerReconfigurationTest.scala covering the corrected behaviour.
