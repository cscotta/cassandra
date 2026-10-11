**Commit**: ff94c44e70 · **Ticket**: KAFKA-18067
**Subsystem**: streams/src
**Category**: Resource leak

**Root cause**
This reverts commit e8837465a5fc478f1c79d1ad475b43e00a39a5d7. The commit that is reverted prevents Kafka Streams from re-initializing
its transactional producer. If an exception that fences the
transactional producer occurs, the producer is not re-initialized during
the handling of the exception. That causes an infinite loop of
ProducerFencedExceptions with corresponding rebalances.

**Before/After**
```java
// Before
        if (!streamsProducer.isClosed())
            streamsProducer.resetProducer(producer());
    private boolean closed = false;
    boolean isClosed() {
        return closed;
    }

        closed = true;
        assertThat(activeTaskCreator.streamsProducer().isClosed(), is(true));
    @Test
    public void shouldNotReInitializeProducerOnClose() {
        properties.put(StreamsConfig.PROCESSING_GUARANTEE_CONFIG, StreamsConfig.EXACTLY_ONCE_V2);

// After
        streamsProducer.resetProducer(producer());
```

**How to spot**
When reviewing streams/src code, look for resource leak patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: hotfix: revert "kafka-18067: kafka streams can leak producer client under eos (#17931)" (#19078).

**Test**
Test modified in ActiveTaskCreatorTest.java covering the corrected behaviour.
