**Commit**: cc20e78474 · **Ticket**: KAFKA-17648
**Subsystem**: clients/src
**Category**: Resource leak

**Root cause**
Reviewers: Lianet Magrans <lmagrans@confluent.io>, Kirk True <ktrue@confluent.io>

**Before/After**
```java
// Before
                // If users subscribe to an invalid topic name, they will get InvalidTopicException in error events,
                // because network thread keeps trying to send MetadataRequest in the background.
                // Ignore it to avoid unsubscribe failed.
                processBackgroundEvents(unsubscribeEvent.future(), timer, e -> e instanceof InvalidTopicException);

// After
import org.apache.kafka.common.errors.GroupAuthorizationException;
import org.apache.kafka.common.errors.TopicAuthorizationException;
                // If users subscribe to a topic with invalid name or without permission, they will get some exceptions.
                // Because network thread keeps trying to send MetadataRequest or ConsumerGroupHeartbeatRequest in the background,
                // there will be some error events in the background queue.
                // When running unsubscribe, these exceptions should be ignored, or users can't unsubscribe successfully.
                processBackgroundEvents(unsubscribeEvent.future(), timer,
                    e -> e instanceof InvalidTopicException || e instanceof TopicAuthorizationException || e instanceof GroupAuthorizationException);
import org.apache.kafka.common.errors.TopicAuthorizationException;
    @Test
    public void testUnsubscribeWithTopicAuthorizationException() {
        consumer = newConsumer();
```

**How to spot**
When reviewing clients/src code, look for resource leak patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-17648: asynckafkaconsumer#unsubscribe and #close swallow topicauthorizationexception and groupauthorizationexcepti.

**Test**
Test modified in AsyncKafkaConsumerTest.java covering the corrected behaviour.
