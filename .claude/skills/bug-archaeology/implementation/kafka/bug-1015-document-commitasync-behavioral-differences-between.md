**Commit**: f9df9b5652 · **Ticket**: KAFKA-19311
**Subsystem**: clients/src
**Category**: Logic error in condition

**Root cause**
#15613 ensures that all `commitAsync` callbacks are triggered before
`commitSync` completes for `AsyncKafkaConsumer`. However, the related
changes to `ClassicKafkaConsumer`, #15693, were not merged. I assume
this might be because we intend to gradually move toward using AsyncConsumer
instead. In short, this behavioral difference should be documented.

**Before/After**
```java
// Before
     * (or similar) are guaranteed to have their callbacks invoked prior to completion of this method.
     * (or similar) are guaranteed to have their callbacks invoked prior to completion of this method.
     * (or similar) are guaranteed to have their callbacks invoked prior to completion of this method.
     * (or similar) are guaranteed to have their callbacks invoked prior to completion of this method.

// After
     * (or similar) are guaranteed to have their callbacks invoked prior to completion of this method,
     * but only when the consumer is using the consumer group protocol.
     * (or similar) are guaranteed to have their callbacks invoked prior to completion of this method,
     * but only when the consumer is using the consumer group protocol.
     * (or similar) are guaranteed to have their callbacks invoked prior to completion of this method,
     * but only when the consumer is using the consumer group protocol.
     * (or similar) are guaranteed to have their callbacks invoked prior to completion of this method,
     * but only when the consumer is using the consumer group protocol.
```

**How to spot**
When reviewing clients/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-19311 document commitasync behavioral differences between classic and async consumer (#19864).

**Test**
No test added.
