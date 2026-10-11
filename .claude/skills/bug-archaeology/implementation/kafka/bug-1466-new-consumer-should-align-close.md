**Commit**: 66363160c5 · **Ticket**: KAFKA-18645
**Subsystem**: clients/src
**Category**: Resource leak

**Root cause**
Reviewers: Lianet Magrans <lmagrans@confluent.io>, Kirk True <ktrue@confluent.io>, Chia-Ping Tsai <chia7712@gmail.com>

**Before/After**
```java
// Before
        final Timer closeTimer = time.timer(timeout);
            () -> runRebalanceCallbacksOnClose(closeTimer), firstException);

// After
     * <p>
     * The actual maximum wait time is bounded by the {@link ConsumerConfig#REQUEST_TIMEOUT_MS_CONFIG} setting, which
     * only applies to operations performed with the broker (coordinator-related requests and
     * fetch sessions). Even if a larger timeout is specified, the consumer will not wait longer than
     * {@link ConsumerConfig#REQUEST_TIMEOUT_MS_CONFIG} for these requests to complete during the close operation.
     * Note that the execution time of callbacks (such as {@link OffsetCommitCallback} and
     * {@link ConsumerRebalanceListener}) does not consume time from the close timeout.
    private final int requestTimeoutMs;
            this.requestTimeoutMs = config.getInt(ConsumerConfig.REQUEST_TIMEOUT_MS_CONFIG);
                       int requestTimeoutMs,
        this.requestTimeoutMs = requestTimeoutMs;
        this.requestTimeoutMs = config.getInt(ConsumerConfig.REQUEST_TIMEOUT_MS_CONFIG);
```

**How to spot**
When reviewing clients/src code, look for resource leak patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-18645: new consumer should align close timeout handling with classic consumer (#18702).

**Test**
Test modified in AsyncKafkaConsumerTest.java covering the corrected behaviour.
