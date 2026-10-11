**Commit**: 66aa9cd282 · **Ticket**: KAFKA-17724
**Subsystem**: clients/src
**Category**: Race condition / missing synchronization

**Root cause**
Reviewers: Jason Gustafson <jason@responsive.dev>, Chia-Ping Tsai <chia7712@gmail.com>

**Before/After**
```java
// Before
import java.util.HashSet;
                log.info("Subscribed to topic(s): {}", String.join(", ", topics));
                if (subscriptions.subscribeToShareGroup(new HashSet<>(topics)))
                    metadata.requestUpdateForNewTopics();

                applicationEventHandler.add(new ShareSubscriptionChangeEvent());
            Timer timer = time.timer(Long.MAX_VALUE);
            applicationEventHandler.add(unsubscribeApplicationEvent);
            log.info("Unsubscribing all topics");
            subscriptions.unsubscribe();
    private void process(final ShareSubscriptionChangeEvent ignored) {
            log.warn("Group membership manager not present when processing a subscribe event");

// After
                applicationEventHandler.addAndGet(new ShareSubscriptionChangeEvent(topics));

                log.info("Subscribed to topics: {}", String.join(", ", topics));
            Timer timer = time.timer(defaultApiTimeoutMs);
            applicationEventHandler.addAndGet(unsubscribeApplicationEvent);
            log.info("Unsubscribed all topics");
    private void process(final ShareSubscriptionChangeEvent event) {
            KafkaException error = new KafkaException("Group membership manager not present when processing a subscribe event");
            event.future().completeExceptionally(error);

        if (subscriptions.subscribeToShareGroup(event.topics()))
            metadata.requestUpdateForNewTopics();
```

**How to spot**
When reviewing clients/src code, look for race condition / missing synchronization patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-17724 move subscriptionstate changes to background thread (#17418).

**Test**
Test modified in ShareConsumerImplTest.java covering the corrected behaviour.
