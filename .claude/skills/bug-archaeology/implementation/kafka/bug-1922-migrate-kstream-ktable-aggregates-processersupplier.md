**Commit**: 184b64fb41 · **Ticket**: KAFKA-18026
**Subsystem**: streams/src
**Category**: Incorrect filtering / wrong query result

**Root cause**
As part of KIP-1112, to maximize the utility of the new ProcessorWrapper, we need to migrate the DSL operators to the new method of attaching state stores by implementing ProcessorSupplier#stores, which makes these stores available for inspection by the user's wrapper. This PR covers the aggregate operator for both KStream and KTable. Reviewers: Guozhang Wang <guozhang.wang.us@gmail.com>, Rohan Desai <rohan@responsive.dev>

**Before/After**
```java
// Before
        processRepartitions(groupPatterns, storeFactory);
                new KStreamAggregate<>(storeFactory.name(), initializer, kGroupedStream.getValue());
        processRepartitions(groupPatterns, storeFactory);
                    storeFactory.name(),
        processRepartitions(groupPatterns, storeFactory);
                    storeFactory.name(),
        processRepartitions(groupPatterns, storeFactory);
                    storeFactory.name(),
                                     final StoreFactory storeFactory) {

// After
        processRepartitions(groupPatterns, storeFactory.name());
                new KStreamAggregate<>(storeFactory, initializer, kGroupedStream.getValue());
        processRepartitions(groupPatterns, storeFactory.name());
                    storeFactory,
        processRepartitions(groupPatterns, storeFactory.name());
                    storeFactory,
        processRepartitions(groupPatterns, storeFactory.name());
                    storeFactory,

```

**How to spot**
When reviewing streams/src code, look for incorrect filtering / wrong query result patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-18026: migrate kstream and ktable aggregates to use processersupplier#stores (#17929).

**Test**
Test modified in StreamsBuilderTest.java covering the corrected behaviour.
