**Commit**: 72532b6f73 · **Ticket**: KAFKA-18376
**Subsystem**: clients/src
**Category**: Logic error in condition

**Root cause**
Introduces `AsyncPollEvent` to make the poll event handling in
AsyncKafkaConsumer and  ApplicationEventProcessor non-blocking to avoid
performance bottlenecks. The new approach enables multi-stage polling
logic, where possible. Reviewers: Lianet Magrans <lmagrans@confluent.io>

**Before/After**
```java
// Before
     * <p>Similarly, we may have to unblock the application thread to send a `PollApplicationEvent` to make sure
import org.apache.kafka.clients.consumer.internals.events.PollEvent;
    // to keep from repeatedly scanning subscriptions in poll(), cache the result during metadata updates
    private boolean cachedSubscriptionHasAllFetchPositions;
                    requestManagersSupplier);
        Supplier<NetworkClientDelegate> networkClientDelegateSupplier = () -> new NetworkClientDelegate(


// After
import org.apache.kafka.clients.consumer.internals.events.AsyncPollEvent;
     * <p>Similarly, we may have to unblock the application thread to send a {@link AsyncPollEvent} to make sure
import org.apache.kafka.clients.consumer.internals.events.AsyncPollEvent;
    private AsyncPollEvent inflightPoll;
                    requestManagersSupplier
            );
        Supplier<NetworkClientDelegate> networkClientDelegateSupplier = NetworkClientDelegate.supplier(
```

**How to spot**
When reviewing clients/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-18376: high cpu load when asynckafkaconsumer uses a small max poll value (#20521).

**Test**
Test modified in KafkaConsumerTest.java covering the corrected behaviour.
