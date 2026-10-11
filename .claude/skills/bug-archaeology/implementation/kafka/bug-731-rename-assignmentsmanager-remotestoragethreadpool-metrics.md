**Commit**: 7d098cfbbd · **Ticket**: KAFKA-17876
**Subsystem**: docs/upgrade.html
**Category**: Logic error in condition

**Root cause**
Rename org.apache.kafka.server:type=AssignmentsManager and
org.apache.kafka.storage.internals.log.RemoteStorageThreadPool metrics
for the consist, these metrics should be

- `kafka.log.remote:type=...`
- `kafka.server:type=...`

Reviewers: Chia-Ping Tsai <chia7712@gmail.com>

**Before/After**
```java
// Before
            metricName("QueuedReplicaToDirAssignments");
    static MetricName metricName(String name) {
        return KafkaYammerMetrics.getMetricName("org.apache.kafka.server", "AssignmentsManager", name);
    }

                @Override
                public Integer value() {
                    return numPending();
                }
            });

// After
    <li>
        The metrics <code>org.apache.kafka.server:type=AssignmentsManager.QueuedReplicaToDirAssignments</code>,
        <code>org.apache.kafka.storage.internals.log:type=RemoteStorageThreadPool.RemoteLogReaderTaskQueueSize</code>, and
        <code>org.apache.kafka.storage.internals.log:type=RemoteStorageThreadPool.RemoteLogReaderAvgIdlePercent</code>
        have been deprecated and will be removed in Kafka 5.0.

        As replacements, the following metrics have been introduced, which report the same information:
        <code>kafka.server:type=AssignmentsManager.QueuedReplicaToDirAssignments</code>,
        <code>kafka.log.remote:type=RemoteStorageThreadPool.RemoteLogReaderTaskQueueSize</code>, and
        <code>kafka.log.remote:type=RemoteStorageThreadPool.RemoteLogReaderAvgIdlePercent</code>.
        For further details, please refer to <a href="https://cwiki.apache.org/confluence/x/6oqMEw">KIP-1100</a>.
    </li>
```

**How to spot**
When reviewing docs/upgrade.html code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-17876/ kafka-19150 rename assignmentsmanager and remotestoragethreadpool metrics (#20265).

**Test**
Test modified in AssignmentsManagerTest.java covering the corrected behaviour.
