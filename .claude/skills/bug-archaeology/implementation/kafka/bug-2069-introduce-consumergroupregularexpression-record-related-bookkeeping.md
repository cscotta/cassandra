**Commit**: 1792b19a05 · **Ticket**: KAFKA-17593
**Subsystem**: group-coordinator/src
**Category**: Logic error in condition

**Root cause**
This patch introduces the ConsumerGroupRegularExpression record (key + value) and updates the `GroupMatadataManager` and the `ConsumerGroup` to bookkeep it appropriately. Note that with this change, regular expressions are counted as subscribers in the `subscribedTopicNames` data structure. This is important because the topic metadata of the group is computed based on it. Reviewers: Jeff Kim <jeff.kim@confluent.io>, Lianet Magrans <lmagrans@confluent.io>

**Before/After**
```java
// Before
// (no removals)

// After
import org.apache.kafka.coordinator.group.generated.ConsumerGroupRegularExpressionKey;
import org.apache.kafka.coordinator.group.generated.ConsumerGroupRegularExpressionValue;
import org.apache.kafka.coordinator.group.modern.consumer.ResolvedRegularExpression;
    /**
     * Creates a ConsumerGroupRegularExpression record.
     *
     * @param groupId                       The consumer group id.
     * @param regex                         The regular expression.
     * @param resolvedRegularExpression     The metadata associated with the regular expression.
     * @return The record.
     */
    public static CoordinatorRecord newConsumerGroupRegularExpressionRecord(
```

**How to spot**
When reviewing group-coordinator/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-17593; [4/n] introduce consumergroupregularexpression record & related bookkeeping in consumergroup (#17694).

**Test**
Test modified in GroupCoordinatorRecordHelpersTest.java covering the corrected behaviour.
