**Commit**: b0e5cdfc57 · **Ticket**: KAFKA-18777
**Subsystem**: core/src
**Category**: Logic error in condition

**Root cause**
Rewrite BrokerMetricNamesTest using ReplicaManager.MetricNames, ensuring that all metrics are always included. This helps prevent issues like PartitionsWithLateTransactionsCount not being correctly included in the test before. Reviewers: Chia-Ping Tsai <chia7712@gmail.com>

**Before/After**
```java
// Before
    val expectedMetricNames = Set(
      "LeaderCount", "PartitionCount", "OfflineReplicaCount", "UnderReplicatedPartitions",
      "UnderMinIsrPartitionCount", "AtMinIsrPartitionCount", "ReassigningPartitions",
      "IsrExpandsPerSec", "IsrShrinksPerSec", "FailedIsrUpdatesPerSec",
      "ProducerIdCount",
    )

// After
    val expectedMetricNames = ReplicaManager.MetricNames
```

**How to spot**
When reviewing core/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-18777 add `partitionswithlatetransactionscount` to brokermetricnamestest (#18869).

**Test**
Test modified in BrokerMetricNamesTest.scala covering the corrected behaviour.
