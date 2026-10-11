**Commit**: 60ce83b2c6 · **Ticket**: KAFKA-18123
**Subsystem**: core/src
**Category**: Logic error in condition

**Root cause**
Reviewers: Chia-Ping Tsai <chia7712@gmail.com>

**Before/After**
```java
// Before
// (no removals)

// After

    // In kraft mode, the StripedReplicaPlacer#initialize includes some randomization,
    // so the replica assignment is not deterministic.
    // If a fetcher thread is not assigned any topic partition, it will not be created.
    // Change the replica assignment to ensure that all fetcher threads are created.
    TestUtils.deleteTopicWithAdmin(adminClients.head, topic, servers, controllerServers)
    val replicaAssignment = Map(
      0 -> Seq(0, 1, 2), 1 -> Seq(1, 2, 0), 2 -> Seq(2, 1, 0), 3 -> Seq(0, 1, 2), 4 -> Seq(1, 2, 0),
      5 -> Seq(2, 1, 0), 6 -> Seq(0, 1, 2), 7 -> Seq(1, 2, 0), 8 -> Seq(2, 1, 0), 9 -> Seq(0, 1, 2))
    TestUtils.createTopicWithAdmin(adminClients.head, topic, servers, controllerServers, replicaAssignment = replicaAssignment)

```

**How to spot**
When reviewing core/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-18123 fix flaky dynamicbrokerreconfigurationtest#testthreadpoolresize (#17986).

**Test**
Test modified in DynamicBrokerReconfigurationTest.scala covering the corrected behaviour.
