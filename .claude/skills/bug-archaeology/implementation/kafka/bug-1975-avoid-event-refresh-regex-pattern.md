**Commit**: 654ebe10f4 · **Ticket**: KAFKA-18071
**Subsystem**: clients/src
**Category**: Logic error in condition

**Root cause**
Reviewers: David Jacot <djacot@confluent.io>, Andrew Schofield <aschofield@confluent.io>

**Before/After**
```java
// Before
        try {
            applicationEventHandler.addAndGet(new UpdatePatternSubscriptionEvent(calculateDeadlineMs(timer)));
        } catch (TimeoutException e) {
            return false;
        } finally {
            timer.update();
            if (subscriptions.hasPatternSubscription()) {
                updatePatternSubscription(metadata.fetch());
            }

// After
        if (subscriptions.hasPatternSubscription()) {
            try {
                applicationEventHandler.addAndGet(new UpdatePatternSubscriptionEvent(calculateDeadlineMs(timer)));
            } catch (TimeoutException e) {
                return false;
            } finally {
                timer.update();
            }
        if (!subscriptions.hasPatternSubscription()) {
            return;
        }
            updatePatternSubscription(metadata.fetch());
```

**How to spot**
When reviewing clients/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-18071: avoid event to refresh regex if no pattern subscription (#17917).

**Test**
Test modified in AsyncKafkaConsumerTest.java covering the corrected behaviour.
