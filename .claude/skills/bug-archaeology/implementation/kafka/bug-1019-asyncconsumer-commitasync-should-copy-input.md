**Commit**: 43f603cfb7 · **Ticket**: KAFKA-19351
**Subsystem**: clients/src
**Category**: Logic error in condition

**Root cause**
`AsyncConsumer#commitAsync` and `AsyncConsumer#commitSync` should copy
the input offsets. Reviewers: Andrew Schofield <aschofield@confluent.io>

**Before/After**
```java
// Before
     * @param offsets A map of offsets by partition with associated metadata
     * @param offsets A map of offsets by partition with associated metadata
     * @param offsets A map of offsets by partition with associate metadata. This map will be copied internally, so it
     *         than or equal to the target timestamp. If the timestamp and offset for a specific partition cannot be found within
     *         than or equal to the target timestamp. If the timestamp and offset for a specific partition cannot be found within


// After
     * @param offsets A map of offsets by partition with associated metadata. This map will be copied internally, so it
     *                is safe to mutate the map after returning.
     * @param offsets A map of offsets by partition with associated metadata. This map will be copied internally, so it
     *                is safe to mutate the map after returning.
     * @param offsets A map of offsets by partition with associated metadata. This map will be copied internally, so it
     *         than or equal to the target timestamp. If the timestamp and offset for a specific partition cannot be found within
     *         than or equal to the target timestamp. If the timestamp and offset for a specific partition cannot be found within

```

**How to spot**
When reviewing clients/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-19351: asyncconsumer#commitasync should copy the input offsets (#19855).

**Test**
Test modified in AsyncKafkaConsumerTest.java covering the corrected behaviour.
