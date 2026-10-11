**Commit**: 05d05e1b5e · **Ticket**: KAFKA-17581
**Subsystem**: clients/src
**Category**: Logic error in condition

**Root cause**
Reviewers: Kirk True <ktrue@confluent.io>, Lianet Magrans <lmagrans@confluent.io>

**Before/After**
```java
// Before
            processBackgroundEvents(unsubscribeEvent.future(), timer);
                processBackgroundEvents(unsubscribeEvent.future(), timer);
     * @param future         Event that contains a {@link CompletableFuture}; it is on this future that the
     *                       application thread will wait for completion
     * @param timer          Overall timer that bounds how long to wait for the event to complete
    <T> T processBackgroundEvents(Future<T> future, Timer timer) {
            boolean hadEvents = processBackgroundEvents();

// After
import org.apache.kafka.common.errors.InvalidTopicException;
import java.util.function.Predicate;
            // If users subscribe to an invalid topic name, they will get InvalidTopicException in error events,
            // because network thread keeps trying to send MetadataRequest in the background.
            // Ignore it to avoid unsubscribe failed.
            processBackgroundEvents(unsubscribeEvent.future(), timer, e -> e instanceof InvalidTopicException);
                // If users subscribe to an invalid topic name, they will get InvalidTopicException in error events,
                // because network thread keeps trying to send MetadataRequest in the background.
                // Ignore it to avoid unsubscribe failed.
                processBackgroundEvents(unsubscribeEvent.future(), timer, e -> e instanceof InvalidTopicException);
     * @param future                    Event that contains a {@link CompletableFuture}; it is on this future that the
     *                                  application thread will wait for completion
```

**How to spot**
When reviewing clients/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-17581: asynckafkaconsumer can't unsubscribe invalid topics (#17244).

**Test**
Test modified in AsyncKafkaConsumerTest.java covering the corrected behaviour.
