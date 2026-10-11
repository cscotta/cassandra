**Commit**: d76238a18f · **Ticket**: KAFKA-17696
**Subsystem**: clients/src
**Category**: Logic error in condition

**Root cause**
Reviewers: Kirk True <ktrue@confluent.io>, Lianet Magrans <lmagrans@confluent.io>

**Before/After**
```java
// Before
import org.apache.kafka.common.errors.InvalidTopicException;
import org.apache.kafka.common.errors.TopicAuthorizationException;
                    backgroundEventHandler);
            backgroundEventHandler
                // If users subscribe to a topic with invalid name or without permission, they will get some exceptions.
                // Because network thread keeps trying to send MetadataRequest or ConsumerGroupHeartbeatRequest in the background,
                // there will be some error events in the background queue.
                processBackgroundEvents(unsubscribeEvent.future(), timer,
                    e -> e instanceof InvalidTopicException || e instanceof TopicAuthorizationException || e instanceof GroupAuthorizationException);
                if (event instanceof CompletableEvent)


// After
                    backgroundEventHandler,
                    false);
            backgroundEventHandler,
            false
                // If users have fatal error, they will get some exceptions in the background queue.
                processBackgroundEvents(unsubscribeEvent.future(), timer, e -> e instanceof GroupAuthorizationException);
import org.apache.kafka.clients.consumer.internals.events.CompletableApplicationEvent;
import java.util.stream.Collectors;
        List<CompletableEvent<?>> uncompletedEvents = applicationEventReaper.uncompletedEvents();
        maybeFailOnMetadataError(uncompletedEvents);
                if (event instanceof CompletableEvent) {
                    // Check if there are any met
```

**How to spot**
When reviewing clients/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-17696 new consumer background operations unaware of metadata errors (#17440).

**Test**
Test modified in AsyncKafkaConsumerTest.java covering the corrected behaviour.
