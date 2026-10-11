**Commit**: ed77397814 · **Ticket**: KAFKA-19062
**Subsystem**: clients/src
**Category**: Resource leak

**Root cause**
Limits waiting when closing a share consumer to request.timeout.ms. Reviewers: Andrew Schofield <aschofield@confluent.io>

**Before/After**
```java
// Before
    private final long defaultApiTimeoutMs;
                      final long defaultApiTimeoutMs,
        final Timer closeTimer = time.timer(timeout);

// After
     * <p>
     * The actual maximum wait time is bounded by the {@link ConsumerConfig#REQUEST_TIMEOUT_MS_CONFIG} setting, which
     * only applies to operations performed with the broker (coordinator-related requests).
     * Even if a larger timeout is specified, the consumer will not wait longer than
     * {@link ConsumerConfig#REQUEST_TIMEOUT_MS_CONFIG} for these requests to complete during the close operation.
     * Note that the execution time of callbacks (such as {@link AcknowledgementCommitCallback}) do not consume time from the close timeout.
    private final int requestTimeoutMs;
    private final int defaultApiTimeoutMs;
            this.requestTimeoutMs = config.getInt(ConsumerConfig.REQUEST_TIMEOUT_MS_CONFIG);
        this.requestTimeoutMs = config.getInt(ConsumerConfig.REQUEST_TIMEOUT_MS_CONFIG);
                      final int requestTimeoutMs,
                      final int defaultApiTimeoutMs,
```

**How to spot**
When reviewing clients/src code, look for resource leak patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-19062: port changes from kafka-18645 to share-consumers (#19335).

**Test**
Test modified in ShareConsumerImplTest.java covering the corrected behaviour.
