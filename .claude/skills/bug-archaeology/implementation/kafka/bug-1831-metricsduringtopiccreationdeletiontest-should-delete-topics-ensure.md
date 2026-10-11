**Commit**: 669d8610a2 · **Ticket**: KAFKA-18228
**Subsystem**: core/src
**Category**: Logic error in condition

**Root cause**
Reviewers: TengYao Chi <kitingiao@gmail.com>, Chia-Ping Tsai <chia7712@gmail.com>

**Before/After**
```java
// Before
            adminZkClient.deleteTopic(t)
            TestUtils.verifyTopicDeletion(zkClient, t, partitionNum, servers)

// After
            deleteTopic(t)
            TestUtils.verifyTopicDeletion(null, t, partitionNum, servers)
```

**How to spot**
When reviewing core/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-18228 the metricsduringtopiccreationdeletiontest should delete topics to ensure that the metrics are recreated (#1.

**Test**
Test modified in MetricsDuringTopicCreationDeletionTest.scala covering the corrected behaviour.
