**Commit**: 14a098b289 · **Ticket**: KAFKA-17600
**Subsystem**: clients/src
**Category**: Logic error in condition

**Root cause**
This PR implements KIP-1094. Reviewers: Andrew Schofield <aschofield@confluent.io>, Kirk True <ktrue@confluent.io>, Lucas Brutschy <lbrutschy@confluent.io>

**Before/After**
```java
// Before
    public static final ConsumerRecords<Object, Object> EMPTY = new ConsumerRecords<>(Collections.emptyMap());
        return new ConsumerRecords<>(results);
        return new ConsumerRecords<>(results);
                    return interceptors.onConsume(new ConsumerRecords<>(fetch.records()));

// After
    public static final ConsumerRecords<Object, Object> EMPTY = new ConsumerRecords<>(Map.of(), Map.of());
    private final Map<TopicPartition, OffsetAndMetadata> nextOffsets;
    @Deprecated
        this(records, Map.of());
    }

    public ConsumerRecords(Map<TopicPartition, List<ConsumerRecord<K, V>>> records, final Map<TopicPartition, OffsetAndMetadata> nextOffsets) {
        this.nextOffsets = Map.copyOf(nextOffsets);
    /**
     * Get the next offsets and metadata corresponding to all topic partitions for which the position have been advanced in this poll call
     * @return the next offsets that the consumer will consume
     */
```

**How to spot**
When reviewing clients/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-17600: add nextoffsets to the consumerrecords (#17414).

**Test**
Test modified in ConsumerRecordsTest.java covering the corrected behaviour.
