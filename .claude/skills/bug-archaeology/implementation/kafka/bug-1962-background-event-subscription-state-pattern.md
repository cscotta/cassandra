**Commit**: 0b081fc310 · **Ticket**: KAFKA-15561
**Subsystem**: clients/src
**Category**: Logic error in condition

**Root cause**
Reviewers: David Jacot <djacot@confluent.io>

**Before/After**
```java
// Before
     * subscription, so it's included in a next heartbeat request sent to the broker. No validation of the pattern is
     * performed by the client (other than null/empty checks).
        maybeThrowInvalidGroupIdException();
        throwIfSubscriptionPatternIsInvalid(pattern);
        log.info("Subscribing to regular expression {}", pattern);

        // TODO: generate event to update subscribed regex so it's included in the next HB.
        NONE, AUTO_TOPICS, AUTO_PATTERN, USER_ASSIGNED, AUTO_TOPICS_SHARE
            ", subscribedPattern=" + subscribedPattern +

// After
import org.apache.kafka.clients.consumer.internals.events.TopicRe2JPatternSubscriptionChangeEvent;
     * subscription state, so it's included in the next heartbeat request sent to the broker.
     * No validation of the pattern is performed by the client (other than null/empty checks).
        acquireAndEnsureOpen();
        try {
            maybeThrowInvalidGroupIdException();
            throwIfSubscriptionPatternIsInvalid(pattern);
            log.info("Subscribing to regular expression {}", pattern);
            applicationEventHandler.addAndGet(new TopicRe2JPatternSubscriptionChangeEvent(
                pattern,
                listener,
                calculateDeadlineMs(time.timer(defaultApiTimeoutMs))));
```

**How to spot**
When reviewing clients/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-15561 [2/n]: background event and subscription state changes for re2j pattern (#17918).

**Test**
Test modified in AsyncKafkaConsumerTest.java covering the corrected behaviour.
