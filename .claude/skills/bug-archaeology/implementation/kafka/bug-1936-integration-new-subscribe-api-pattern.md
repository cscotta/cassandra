**Commit**: 6237325fb1 · **Ticket**: KAFKA-15561
**Subsystem**: clients/src
**Category**: Logic error in condition

**Root cause**
- integration tests for new subscribe api with RE2J pattern
- fix to ensure all topics are included in metadata requests when consumer is subscribed to RE2J pattern

Reviewers: David Jacot <djacot@confluent.io>, Chia-Ping Tsai <chia7712@gmail.com>

**Before/After**
```java
// Before
        if (subscription.hasPatternSubscription())
import org.apache.kafka.common.errors.InvalidTopicException

// After
        if (subscription.hasPatternSubscription() || subscription.hasRe2JPatternSubscription())
import org.apache.kafka.common.errors.{InvalidRegularExpression, InvalidTopicException}
  @ParameterizedTest(name = TestInfoUtils.TestWithParameterizedQuorumAndGroupProtocolNames)
  @MethodSource(Array("getTestQuorumAndGroupProtocolParametersConsumerGroupProtocolOnly"))
  def testRe2JPatternSubscription(quorum: String, groupProtocol: String): Unit = {
    val topic1 = "tblablac" // matches subscribed pattern
    createTopic(topic1, 2, brokerCount)

    val topic2 = "tblablak" // does not match subscribed pattern
    createTopic(topic2, 2, brokerCount)

    val topic3 = "tblab1" // does not match subscribed pattern
```

**How to spot**
When reviewing clients/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-15561 [5/n]: integration tests for new subscribe api with re2j pattern (#17964).

**Test**
Test modified in PlaintextConsumerSubscriptionTest.scala covering the corrected behaviour.
