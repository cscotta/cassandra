**Commit**: 42e7cbb67e · **Ticket**: KAFKA-18690
**Subsystem**: clients/src
**Category**: Logic error in condition

**Root cause**
Reviewers: Lianet Magrans <lmagrans@confluent.io>

**Before/After**
```java
// Before
        return subscription.matchesSubscribedPattern(topic);

// After
        return subscription.matchesSubscribedPattern(topic) || subscription.isAssignedFromRe2j(topic);
    public synchronized boolean isAssignedFromRe2j(String topic) {
        if (!hasRe2JPatternSubscription()) {
            return false;
        }

        for (TopicPartition topicPartition : assignment.partitionSet()) {
            if (topicPartition.topic().equals(topic)) {
                return true;
            }
        }

```

**How to spot**
When reviewing clients/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-18690: keep leader metadata for re2j-assigned partitions (#18777).

**Test**
Test modified in PlaintextConsumerSubscriptionTest.scala covering the corrected behaviour.
