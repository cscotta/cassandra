**Commit**: 0ba6f3212d · **Ticket**: KAFKA-17903
**Subsystem**: clients/src
**Category**: Logic error in condition

**Root cause**
Reviewers: Chia-Ping Tsai <chia7712@gmail.com>

**Before/After**
```java
// Before
 * (using {@link KafkaFuture#thenApply(KafkaFuture.Function)}, for example) doesn't block
    /**
     * A function which takes objects of type A and returns objects of type B.
     *
     * @deprecated Since Kafka 3.0. Use the {@link BaseFunction} functional interface.
     */
    @Deprecated
    public abstract static class Function<A, B> implements BaseFunction<A, B> { }

    /**
     * Prefer {@link KafkaFuture#thenApply(BaseFunction)} as this function is here for backwards compatibility reasons
     * and might be deprecated/removed in a future release.

// After
 * (using {@link KafkaFuture#thenApply(KafkaFuture.BaseFunction)}, for example) doesn't block
```

**How to spot**
When reviewing clients/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-17903 remove kafkafuture#function and kafkafuture#thenapply (#17644).

**Test**
No test added.
