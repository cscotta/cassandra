**Commit**: 33318bc0f4 · **Ticket**: KAFKA-18000
**Subsystem**: core/src
**Category**: Logic error in condition

**Root cause**
Reviewers: David Arthur <mumrah@gmail.com>

**Before/After**
```java
// Before
import org.apache.kafka.common.test.api.Flaky
  @Flaky("KAFKA-18000")
      assertTrue(brokerTopicStats.topicStats(tp0.topic()).buildRemoteLogAuxStateRequestRate.count > 0)
      assertTrue(brokerTopicStats.allTopicsStats.buildRemoteLogAuxStateRequestRate.count > 0)

// After
      waitUntilTrue(() => brokerTopicStats.topicStats(tp0.topic()).buildRemoteLogAuxStateRequestRate.count > 0,
        "Should have buildRemoteLogAuxStateRequestRate count > 0, but got:" + brokerTopicStats.topicStats(tp0.topic()).buildRemoteLogAuxStateRequestRate.count)
      waitUntilTrue(() => brokerTopicStats.allTopicsStats.buildRemoteLogAuxStateRequestRate.count > 0,
        "Should have all topic buildRemoteLogAuxStateRequestRate count > 0, but got:" + brokerTopicStats.allTopicsStats.buildRemoteLogAuxStateRequestRate.count)
```

**How to spot**
When reviewing core/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-18000: fix flaky replicamanagertest#testsuccessfulbuildremotelogauxstatemetrics (#17887).

**Test**
Test modified in ReplicaManagerTest.scala covering the corrected behaviour.
