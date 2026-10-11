**Commit**: 37b4d9b01d · **Ticket**: KAFKA-15561
**Subsystem**: clients/src
**Category**: Logic error in condition

**Root cause**
Reviewers: David Jacot <djacot@confluent.io>

**Before/After**
```java
// Before
            // ClientAssignors - not supported yet


// After
            case INVALID_REGULAR_EXPRESSION:
                logger.error("{} failed due to {}: {}", heartbeatRequestName(), error, errorMessage);
                handleFatalFailure(error.exception("Invalid RE2J SubscriptionPattern provided in the call to " +
                    "subscribe. " + errorMessage));
                break;

import org.apache.kafka.clients.consumer.SubscriptionPattern;
import java.util.Objects;
            // SubscribedTopicRegex - only sent if it has changed since the last heartbeat.
            // Send empty string to indicate that a subscribed pattern needs to be removed.
            SubscriptionPattern pattern = subscriptions.subscriptionPattern();
            boolean patternUpdated = !Objects.equals(pattern, sentFields.pattern);
```

**How to spot**
When reviewing clients/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-15561 [3/n]: client support for subscriptionpattern in hb (#17951).

**Test**
Test modified in ConsumerHeartbeatRequestManagerTest.java covering the corrected behaviour.
