**Commit**: 3268435fd6 · **Ticket**: KAFKA-18013
**Subsystem**: clients/src
**Category**: Logic error in condition

**Root cause**
- Deprecates OffsetResetStrategy enum
- Adds new internal class AutoOffsetResetStrategy
- Replaces all OffsetResetStrategy enum usages with AutoOffsetResetStrategy
- Deprecate old/Add new constructors to MockConsumer

 Reviewers: Andrew Schofield <aschofield@confluent.io>, Matthias J. Sax <matthias@confluent.io>

**Before/After**
```java
// Before
                                        OffsetResetStrategy.LATEST.toString(),
                                        in(Utils.enumOptions(OffsetResetStrategy.class)),
        subscriptions.requestOffsetReset(partitions, OffsetResetStrategy.EARLIEST);
        subscriptions.requestOffsetReset(partitions, OffsetResetStrategy.LATEST);
        OffsetResetStrategy strategy = subscriptions.resetStrategy(tp);
        if (strategy == OffsetResetStrategy.EARLIEST) {
        } else if (strategy == OffsetResetStrategy.LATEST) {

// After
import org.apache.kafka.clients.consumer.internals.AutoOffsetResetStrategy;
                                        AutoOffsetResetStrategy.LATEST.name(),
                                        new AutoOffsetResetStrategy.Validator(),
import org.apache.kafka.clients.consumer.internals.AutoOffsetResetStrategy;
    /**
     * @deprecated Since 4.0. Use {@link #MockConsumer(String)}.
     */
    @Deprecated
        this(AutoOffsetResetStrategy.fromString(offsetResetStrategy.toString()));
    }

    /**
```

**How to spot**
When reviewing clients/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-18013: add autooffsetresetstrategy internal class (#17858).

**Test**
Test modified in KafkaConsumerTest.java covering the corrected behaviour.
