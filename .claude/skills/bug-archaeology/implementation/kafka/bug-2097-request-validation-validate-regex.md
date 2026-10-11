**Commit**: 64f3ee4c33 · **Ticket**: KAFKA-17593
**Subsystem**: checkstyle/import-control-group-coordinator.xml
**Category**: State not cleaned up

**Root cause**
This patch does two things:
1) Change the validation of the ConsumerGroupHeartbeat request to accept subscribed topic names and/or subscribed topic regex. At least of them must be set in the first request with epoch 0. 2) Validate the provided regular expression by compiling it. Co-authored-by: Lianet Magrans <lmagrans@confluent.io>

Reviewers: Jeff Kim <jeff.kim@confluent.io>, Lianet Magrans <lmagrans@confluent.io>

**Before/After**
```java
// Before
// (no removals)

// After
            <allow pkg="com.google.re2j" />
  @ClusterTest
  def testConsumerGroupHeartbeatWithRegularExpression(): Unit = {
    val raftCluster = cluster.asInstanceOf[RaftClusterInstance]
    val admin = cluster.createAdminClient()

    // Creates the __consumer_offsets topics because it won't be created automatically
    // in this test because it does not use FindCoordinator API.
    TestUtils.createOffsetsTopicWithAdmin(
      admin = admin,
      brokers = raftCluster.brokers.values().asScala.toSeq,
      controllers = raftCluster.controllers().values().asScala.toSeq
```

**How to spot**
When reviewing checkstyle/import-control-group-coordinator.xml code, look for state not cleaned up patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-17593; [2/n] update request validation & validate regex (#17651).

**Test**
Test modified in ConsumerGroupHeartbeatRequestTest.scala covering the corrected behaviour.
