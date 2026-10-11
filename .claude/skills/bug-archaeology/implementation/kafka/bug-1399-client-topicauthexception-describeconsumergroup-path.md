**Commit**: c580874fc2 · **Ticket**: KAFKA-18813
**Subsystem**: clients/src
**Category**: Resource leak

**Root cause**
Reviewers: David Jacot <djacot@confluent.io>

**Before/After**
```java
// Before
// (no removals)

// After
            case TOPIC_AUTHORIZATION_FAILED:
                // The topic auth response received on DescribeConsumerGroup is a generic one not including topic names, so we just pass it on unchanged here.
import org.apache.kafka.common.errors.TopicAuthorizationException;
        assertFailed(TopicAuthorizationException.class, handleConsumerGroupWithError(Errors.TOPIC_AUTHORIZATION_FAILED));
```

**How to spot**
When reviewing clients/src code, look for resource leak patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-18813: [3/n] client support for topicauthexception in describeconsumergroup path (#18996).

**Test**
Test modified in DescribeConsumerGroupsHandlerTest.java covering the corrected behaviour.
