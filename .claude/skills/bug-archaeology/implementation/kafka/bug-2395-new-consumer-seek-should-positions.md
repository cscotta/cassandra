**Commit**: 3f4c25fe1d · **Ticket**: KAFKA-17448
**Subsystem**: clients/src
**Category**: Logic error in condition

**Root cause**
Reviewers: Lianet Magrans <lmagrans@confluent.io>, Kirk True <ktrue@confluent.io>

**Before/After**
```java
// Before
import org.apache.kafka.clients.Metadata;
            SubscriptionState.FetchPosition newPosition = new SubscriptionState.FetchPosition(
                offset,
                Optional.empty(), // This will ensure we skip validation
                metadata.currentLeader(partition));
            subscriptions.seekUnvalidated(partition, newPosition);
            Metadata.LeaderAndEpoch currentLeaderAndEpoch = metadata.currentLeader(partition);
            SubscriptionState.FetchPosition newPosition = new SubscriptionState.FetchPosition(
                offsetAndMetadata.offset(),
                offsetAndMetadata.leaderEpoch(),
                currentLeaderAndEpoch);
            subscriptions.seekUnvalidated(partition, newPosition);

// After
import org.apache.kafka.clients.consumer.internals.events.SeekUnvalidatedEvent;
            Timer timer = time.timer(defaultApiTimeoutMs);
            SeekUnvalidatedEvent seekUnvalidatedEventEvent = new SeekUnvalidatedEvent(
                    calculateDeadlineMs(timer), partition, offset, Optional.empty());
            applicationEventHandler.addAndGet(seekUnvalidatedEventEvent);

            Timer timer = time.timer(defaultApiTimeoutMs);
            SeekUnvalidatedEvent seekUnvalidatedEventEvent = new SeekUnvalidatedEvent(
                    calculateDeadlineMs(timer), partition, offsetAndMetadata.offset(), offsetAndMetadata.leaderEpoch());
            applicationEventHandler.addAndGet(seekUnvalidatedEventEvent);
        SHARE_ACKNOWLEDGE_ON_CLOSE,
        SEEK_UNVALIDATED,
```

**How to spot**
When reviewing clients/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-17448: new consumer seek should update positions in background thread (#17075).

**Test**
Test modified in AsyncKafkaConsumerTest.java covering the corrected behaviour.
