**Commit**: e8837465a5 · **Ticket**: KAFKA-18067
**Subsystem**: streams/src
**Category**: Resource leak

**Root cause**
To avoid leaking producers, we should add a 'closedflag toStreamProducer` indicating whether we should reset prouder. Reviewers: Guozhang Wang <guozhang.wang.us@gmail.com>, Anna Sophie Blee-Goldman <ableegoldman@apache.org>

**Before/After**
```java
// Before
        streamsProducer.resetProducer(producer());

// After
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
```

**How to spot**
When reviewing streams/src code, look for resource leak patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-18067: kafka streams can leak producer client under eos (#17931).

**Test**
Test modified in ActiveTaskCreatorTest.java covering the corrected behaviour.
