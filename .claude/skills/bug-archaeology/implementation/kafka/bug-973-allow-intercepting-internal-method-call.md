**Commit**: 0adc6fa3e1 · **Ticket**: KAFKA-19271
**Subsystem**: streams/src
**Category**: Logic error in condition

**Root cause**
To allow intercepting the internal subscribe call to the async-consumer,
we need to extend ConsumerWrapper interface accordingly, instead of
returning the wrapped async-consumer back to the KS runtime. Reviewers: Lucas Brutschy <lbrutschy@confluent.io>

**Before/After**
```java
// Before
    public AsyncKafkaConsumer<byte[], byte[]> consumer() {
        return delegate;
    }

                final AsyncKafkaConsumer<byte[], byte[]> consumer = mainConsumer instanceof ConsumerWrapper
                    ? ((ConsumerWrapper) mainConsumer).consumer()
                    : (AsyncKafkaConsumer<byte[], byte[]>) mainConsumer;
                consumer.subscribe(
                    topologyMetadata.allFullSourceTopicNames(),
                    new DefaultStreamsRebalanceListener(
                        log,
                        time,

// After
import org.apache.kafka.clients.consumer.internals.StreamsRebalanceListener;
    public void subscribe(final Collection<String> topics, final StreamsRebalanceListener streamsRebalanceListener) {
        delegate.subscribe(topics, streamsRebalanceListener);
    }

                if (mainConsumer instanceof ConsumerWrapper) {
                    ((ConsumerWrapper) mainConsumer).subscribe(
                        topologyMetadata.allFullSourceTopicNames(),
                        new DefaultStreamsRebalanceListener(
                            log,
                            time,
                            streamsRebalanceData.get(),
```

**How to spot**
When reviewing streams/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-19271: allow intercepting internal method call (#19832).

**Test**
Test modified in StreamThreadTest.java covering the corrected behaviour.
