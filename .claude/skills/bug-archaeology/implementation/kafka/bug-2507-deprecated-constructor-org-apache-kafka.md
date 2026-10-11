**Commit**: e750f44cf8 · **Ticket**: KAFKA-17409
**Subsystem**: clients/src
**Category**: Logic error in condition

**Root cause**
Reviewers: Chia-Ping Tsai <chia7712@gmail.com>

**Before/After**
```java
// Before
    /**
     * Creates a new instance with the provided parameters.
     *
     * @deprecated use constructor without `checksum` parameter. This constructor will be removed in
     *             Apache Kafka 4.0 (deprecated since 3.0).
     */
    @Deprecated
    public RecordMetadata(TopicPartition topicPartition, long baseOffset, long batchIndex, long timestamp,
                          Long checksum, int serializedKeySize, int serializedValueSize) {
        this(topicPartition, baseOffset, batchIndexToInt(batchIndex), timestamp, serializedKeySize, serializedValueSize);
    }


// After
// (no additions)
```

**How to spot**
When reviewing clients/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-17409 remove deprecated constructor of org.apache.kafka.clients.producer.recordmetadata (#16979).

**Test**
Test modified in RecordMetadataTest.java covering the corrected behaviour.
