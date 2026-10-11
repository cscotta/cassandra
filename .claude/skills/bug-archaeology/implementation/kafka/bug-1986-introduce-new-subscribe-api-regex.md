**Commit**: 0d7c765981 · **Ticket**: KAFKA-15561
**Subsystem**: clients/src
**Category**: Resource leak

**Root cause**
Reviewers: David Jacot <djacot@confluent.io>

**Before/After**
```java
// Before
// (no removals)

// After
    /**
     * @see KafkaConsumer#subscribe(SubscriptionPattern, ConsumerRebalanceListener)
     */
    void subscribe(SubscriptionPattern pattern, ConsumerRebalanceListener callback);

    /**
     * @see KafkaConsumer#subscribe(SubscriptionPattern)
     */
    void subscribe(SubscriptionPattern pattern);

import org.apache.kafka.common.errors.InvalidRegularExpression;
    /**
```

**How to spot**
When reviewing clients/src code, look for resource leak patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-15561 [1/n]: introduce new subscribe api for re2j regex (#17897).

**Test**
Test modified in KafkaConsumerTest.java covering the corrected behaviour.
