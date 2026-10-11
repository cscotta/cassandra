**Commit**: a24fedfba0 · **Ticket**: KAFKA-18817
**Subsystem**: clients/src
**Category**: Resource leak

**Root cause**
1、Client support for TopicAuthException in DescribeShareGroup and HB
path
2、ShareConsumerImpl#sendAcknowledgementsAndLeaveGroup swallow
TopicAuthorizationException and GroupAuthorizationException

Reviewers: ShivsundarR <shr@confluent.io>, Andrew Schofield <aschofield@confluent.io>

**Before/After**
```java
// Before
            processBackgroundEvents(unsubscribeEvent.future(), timer);
     * @param future         Event that contains a {@link CompletableFuture}; it is on this future that the
     *                       application thread will wait for completion
     * @param timer          Overall timer that bounds how long to wait for the event to complete
                                  final Timer timer) {
            boolean hadEvents = processBackgroundEvents();

// After
            case TOPIC_AUTHORIZATION_FAILED:
                // The topic auth response received on DescribeShareGroup is a generic one not including topic names, so we just pass it on unchanged here.
import org.apache.kafka.common.errors.GroupAuthorizationException;
import org.apache.kafka.common.errors.TopicAuthorizationException;
import java.util.function.Predicate;
            // If users have fatal error, they will get some exceptions in the background queue.
            // When running unsubscribe, these exceptions should be ignored, or users can't unsubscribe successfully.
            processBackgroundEvents(unsubscribeEvent.future(), timer, e -> (e instanceof GroupAuthorizationException
                || e instanceof TopicAuthorizationException));
     * @param future                    Event that contains a {@link CompletableFuture}; it is on this future that the
     *                                  application thread will wait for completion
     * @param timer                     Overall timer that bounds how long to wait for the event to complete
```

**How to spot**
When reviewing clients/src code, look for resource leak patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-18817:[1/n] sharegroupheartbeat and sharegroupdescribe api must check topic describe (#19055).

**Test**
Test modified in ShareConsumerImplTest.java covering the corrected behaviour.
