**Commit**: 5cf91e4cbe · **Ticket**: KAFKA-17593
**Subsystem**: group-coordinator/src
**Category**: Logic error in condition

**Root cause**
This patch adds a data structure to ConsumerGroup to track the number of members subscribed to each regular expressions in the group. This will be useful to know whether a regex is new in the group or whether a regex must be removed from the group. Reviewers: Jeff Kim <jeff.kim@confluent.io>, Lianet Magrans <lmagrans@confluent.io>

**Before/After**
```java
// Before
        Uuid topicId, int partitionId

// After
    /**
     * The number of members subscribed to each regular expressions.
     */
    private final TimelineHashMap<String, Integer> subscribedRegularExpressions;

        this.subscribedRegularExpressions = new TimelineHashMap<>(snapshotRegistry, 0);
        maybeUpdateSubscribedRegularExpression(oldMember, newMember);
        maybeUpdateSubscribedRegularExpression(oldMember, null);
    /**
     * @return The number of members subscribed to the provided regex.
     */
    public int numSubscribedMembers(String regex) {
```

**How to spot**
When reviewing group-coordinator/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-17593; [3/n] track the number of subscribed members per regular expression in consumergroup (#17653).

**Test**
Test modified in ConsumerGroupTest.java covering the corrected behaviour.
