**Commit**: 13f497fe87 · **Ticket**: KAFKA-18009
**Subsystem**: clients/src
**Category**: Wrong serialization / deserialization

**Root cause**
KafkaShareConsumer had a constructor which was marked as public in error and should have been package-private.

**Before/After**
```java
// Before
    public KafkaShareConsumer(ConsumerConfig config,

// After
    KafkaShareConsumer(ConsumerConfig config,
```

**How to spot**
When reviewing clients/src code, look for wrong serialization / deserialization patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-18009: remove extra public constructor for kafkashareconsumer (#17806).

**Test**
No test added.
