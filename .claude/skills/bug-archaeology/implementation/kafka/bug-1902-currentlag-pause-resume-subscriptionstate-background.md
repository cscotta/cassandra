**Commit**: 4362ab7090 · **Ticket**: KAFKA-17947
**Subsystem**: clients/src
**Category**: Logic error in condition

**Root cause**
Reviewers: Lianet Magrans <lmagrans@confluent.io>

**Before/After**
```java
// Before
    private final int defaultApiTimeoutMs;
            this.defaultApiTimeoutMs = config.getInt(ConsumerConfig.DEFAULT_API_TIMEOUT_MS_CONFIG);
        this.defaultApiTimeoutMs = defaultApiTimeoutMs;
        this.defaultApiTimeoutMs = config.getInt(ConsumerConfig.DEFAULT_API_TIMEOUT_MS_CONFIG);
        commitSync(Duration.ofMillis(defaultApiTimeoutMs));
            Timer timer = time.timer(defaultApiTimeoutMs);
                    calculateDeadlineMs(timer), partition, offset, Optional.empty());
            Timer timer = time.timer(defaultApiTimeoutMs);
            SeekUnvalidatedEvent seekUnvalidatedEventEvent = new SeekUnvalidatedEvent(
                    calculateDeadlineMs(timer), partition, offsetAndMetadata.offset(), offsetAndMetadata.leaderEpoch());
            application

// After
import org.apache.kafka.clients.consumer.internals.events.CurrentLagEvent;
import org.apache.kafka.clients.consumer.internals.events.PausePartitionsEvent;
import org.apache.kafka.clients.consumer.internals.events.ResumePartitionsEvent;
    private final Duration defaultApiTimeoutMs;
            this.defaultApiTimeoutMs = Duration.ofMillis(config.getInt(ConsumerConfig.DEFAULT_API_TIMEOUT_MS_CONFIG));
        this.defaultApiTimeoutMs = Duration.ofMillis(defaultApiTimeoutMs);
        this.defaultApiTimeoutMs = Duration.ofMillis(config.getInt(ConsumerConfig.DEFAULT_API_TIMEOUT_MS_CONFIG));
        commitSync(defaultApiTimeoutMs);
                defaultApiTimeoutDeadlineMs(),
                partition,
                offset,
                Optional.empty()
```

**How to spot**
When reviewing clients/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-17947: update currentlag(), pause(), and resume() to update subscriptionstate in background thread (#17699).

**Test**
No test added.
