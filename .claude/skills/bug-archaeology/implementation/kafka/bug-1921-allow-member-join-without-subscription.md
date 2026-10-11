**Commit**: 275b995bf2 · **Ticket**: KAFKA-18095
**Subsystem**: group-coordinator/src
**Category**: Logic error in condition

**Root cause**
This patch relaxes requiring non-empty subscribed names and regex in the full heartbeat request. Without this, a consumer using client side regexes may not be able to join the group when the regex does not match any topics yet and this is inconsistent with the old protocol. Relaxing the subscribed regex is not strictly required but it seems better to keep it consistent. Reviewers: Lianet Magrans <lmagrans@confluent.io>

**Before/After**
```java
// Before
            boolean hasSubscribedTopicNames = request.subscribedTopicNames() != null && !request.subscribedTopicNames().isEmpty();
            boolean hasSubscribedTopicRegex = request.subscribedTopicRegex() != null && !request.subscribedTopicRegex().isEmpty();
            if (!hasSubscribedTopicNames && !hasSubscribedTopicRegex) {
                throw new InvalidRequestException("SubscribedTopicNames or SubscribedTopicRegex must be set in first request.");

// After
  @ClusterTest
  def testConsumerGroupHeartbeatWithEmptySubscription(): Unit = {
    val admin = cluster.admin()

    // Creates the __consumer_offsets topics because it won't be created automatically
    // in this test because it does not use FindCoordinator API.
    try {
      TestUtils.createOffsetsTopicWithAdmin(
        admin = admin,
        brokers = cluster.brokers.values().asScala.toSeq,
        controllers = cluster.controllers().values().asScala.toSeq
      )
```

**How to spot**
When reviewing group-coordinator/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-18095; allow a  member to join without subscription under new consumer protocol (#18003).

**Test**
Test modified in ConsumerGroupHeartbeatRequestTest.scala covering the corrected behaviour.
