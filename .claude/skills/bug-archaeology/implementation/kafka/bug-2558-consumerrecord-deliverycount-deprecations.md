**Commit**: 7031855570 · **Ticket**: KAFKA-17318
**Subsystem**: clients/src
**Category**: Logic error in condition

**Root cause**
Reviewers: Apoorv Mittal <apoorvmittal10@gmail.com>, Chia-Ping Tsai <chia7712@gmail.com>

**Before/After**
```java
// Before
    /**
     * @deprecated checksums are no longer exposed by this class, this constant will be removed in Apache Kafka 4.0
     *             (deprecated since 3.0).
     */
    @Deprecated
    public static final int NULL_CHECKSUM = -1;

     * Creates a record to be received from a specified topic and partition
        if (topic == null)
            throw new IllegalArgumentException("Topic cannot be null");
        if (headers == null)
            throw new IllegalArgumentException("Headers cannot be null");

// After
    private final Optional<Short> deliveryCount;
     * Creates a record to be received from a specified topic and partition.
        this(topic, partition, offset, timestamp, timestampType, serializedKeySize, serializedValueSize, key, value,
            headers, leaderEpoch, Optional.empty());
```

**How to spot**
When reviewing clients/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-17318 consumerrecord.deliverycount and remove deprecations (#16872).

**Test**
Test modified in ConsumerRecordTest.java covering the corrected behaviour.
