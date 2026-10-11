**Commit**: 10f26c8629 · **Ticket**: KAFKA-18836
**Subsystem**: clients/src
**Category**: Logic error in condition

**Root cause**
[KIP-1136](https://cwiki.apache.org/confluence/display/KAFKA/KIP-1136%3A+Make+ConsumerGroupMetadata+an+interface)
Marking ConsumerGroupMetadata class constructors deprecated. since 4.2.0

Reviewers: Andrew Schofield <aschofield@confluent.io>

**Before/After**
```java
// Before
// (no removals)

// After
    /**
     * @deprecated Since 4.2, please use {@link KafkaConsumer#groupMetadata()} instead. This class will be an interface in Kafka 5.0.
     */
    @Deprecated(since = "4.2", forRemoval = true)
    /**
     * @deprecated Since 4.2, please use {@link KafkaConsumer#groupMetadata()} instead. This class will be an interface in Kafka 5.0.
     */
    @Deprecated(since = "4.2", forRemoval = true)
    @SuppressWarnings("removal")
    @SuppressWarnings("removal")
    @SuppressWarnings("removal")
    @SuppressWarnings("removal")
```

**How to spot**
When reviewing clients/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-18836: kip-1136 annotate constructors in consumergroupmetadata @deprecated (#20728).

**Test**
Test modified in ConsumerGroupMetadataTest.java covering the corrected behaviour.
