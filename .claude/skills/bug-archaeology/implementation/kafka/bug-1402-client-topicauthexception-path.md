**Commit**: c56c9faee2 · **Ticket**: KAFKA-18813
**Subsystem**: clients/src
**Category**: Resource leak

**Root cause**
Reviewers: David Jacot <djacot@confluent.io>

**Before/After**
```java
// Before
                processBackgroundEvents(unsubscribeEvent.future(), timer, e -> e instanceof GroupAuthorizationException);

// After
            case TOPIC_AUTHORIZATION_FAILED:
                logger.error("{} failed for member {} with state {} due to {}: {}", heartbeatRequestName(),
                        membershipManager().memberId, membershipManager().state, error, errorMessage);
                // Propagate auth error received in HB so that it's returned on poll.
                // Member should stay in its current state so it can recover if ever the missing ACLs are added.
                backgroundEventHandler.add(new ErrorEvent(error.exception()));
                break;

import org.apache.kafka.common.errors.TopicAuthorizationException;
                processBackgroundEvents(unsubscribeEvent.future(), timer, e -> (e instanceof GroupAuthorizationException || e instanceof TopicAuthorizationException));
            case TOPIC_AUTHORIZATION_FAILED:
                verify(backgroundEventHandler).add(any(ErrorEvent.class));
```

**How to spot**
When reviewing clients/src code, look for resource leak patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-18813: [2/n] client support for topicauthexception in hb path (#18986).

**Test**
Test modified in ConsumerHeartbeatRequestManagerTest.java covering the corrected behaviour.
