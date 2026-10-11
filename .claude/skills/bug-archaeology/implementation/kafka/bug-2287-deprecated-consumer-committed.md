**Commit**: 696e33ee6d · **Ticket**: KAFKA-17451
**Subsystem**: clients/src
**Category**: Resource leak

**Root cause**
Reviewers: Chia-Ping Tsai <chia7712@gmail.com>

**Before/After**
```java
// Before
    /**
     * @see KafkaConsumer#committed(TopicPartition)
     */
    @Deprecated
    OffsetAndMetadata committed(TopicPartition partition);

    /**
     * @see KafkaConsumer#committed(TopicPartition, Duration)
     */
    @Deprecated
    OffsetAndMetadata committed(TopicPartition partition, final Duration timeout);


// After
// (no additions)
```

**How to spot**
When reviewing clients/src code, look for resource leak patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-17451 remove deprecated consumer#committed (#17320).

**Test**
No test added.
