**Commit**: fb65dfeb11 · **Ticket**: KAFKA-17726
**Subsystem**: clients/src
**Category**: Logic error in condition

**Root cause**
Reviewers: Andrew Schofield <aschofield@confluent.io>, Lianet Magrans <lmagrans@confluent.io>

**Before/After**
```java
// Before
import org.apache.kafka.clients.consumer.internals.events.SubscriptionChangeEvent;
    private int metadataVersionSnapshot;
            this.metadataVersionSnapshot = metadata.updateVersion();
        this.metadataVersionSnapshot = metadata.updateVersion();
        this.metadataVersionSnapshot = metadata.updateVersion();
    /**
     * <p>
     *
     * This function evaluates the regex that the consumer subscribed to
     * against the list of topic names from metadata, and updates
     * the list of topics in subscription state accordingly
     *

// After
import org.apache.kafka.clients.consumer.internals.events.TopicPatternSubscriptionChangeEvent;
import org.apache.kafka.clients.consumer.internals.events.TopicSubscriptionChangeEvent;
import org.apache.kafka.clients.consumer.internals.events.UpdatePatternSubscriptionEvent;
        try {
            applicationEventHandler.addAndGet(new UpdatePatternSubscriptionEvent(calculateDeadlineMs(timer)));
        } catch (TimeoutException e) {
            return false;
        } finally {
            timer.update();
        }
            applicationEventHandler.addAndGet(new TopicP
```

**How to spot**
When reviewing clients/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-17726: new consumer subscribe/subscribefrompattern in background thread (#17569).

**Test**
Test modified in AsyncKafkaConsumerTest.java covering the corrected behaviour.
