**Commit**: ef2e4600f3 · **Ticket**: KAFKA-18026
**Subsystem**: streams/src
**Category**: Logic error in condition

**Root cause**
Covers wrapping of processors and state stores for KStream-KStream joins. Includes self-joins and the spurious results fix optimization

Reviewers: Guozhang Wang <guozhang.wang.us@gmail.com>

**Before/After**
```java
// Before
                    if (child.nodeName().equals(joinNode.thisWindowedStreamProcessorParameters().processorName())) {
                    } else if (child.nodeName().equals(joinNode.otherWindowedStreamProcessorParameters().processorName())) {
        final KStreamJoinWindow<K, V1> thisWindowedStream = new KStreamJoinWindow<>(thisWindowStore.storeName());
        final KStreamJoinWindow<K, V2> otherWindowedStream = new KStreamJoinWindow<>(otherWindowStore.storeName());
            otherWindowStore.storeName(),
            outerJoinWindowStore.map(StoreFactory::storeName),
            sharedTimeTrackerSupplier
            thisWindowStore.storeName(),
            outerJoinWindowStore.map(StoreFactory::storeName),
            sharedTimeTrackerSupplier
            thisWindowStore.storeName(),
                   .withThisWindowStoreBuilder(thisWindowStore)

// After
                    if (child.nodeName().equals(joinNode.thisWindowedStreamProcessorName())) {
                    } else if (child.nodeName().equals(joinNode.otherWindowedStreamProcessorName())) {
        final KStreamJoinWindow<K, V1> thisWindowedStream = new KStreamJoinWindow<>(thisWindowStore);
        final KStreamJoinWindow<K, V2> otherWindowedStream = new KStreamJoinWindow<>(otherWindowStore);
            sharedTimeTrackerSupplier,
            otherWindowStore,
            outerJoinWindowStore
            sharedTimeTrackerSupplier,
            thisWindowStore,
            outerJoinWindowStore
            thisWindowStore,
                   .withSelfJoinProcessorParameters(selfJoinProcessorParams)
```

**How to spot**
When reviewing streams/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-18026: kip-1112, migrate stream-stream joins to use processersupplier#stores (#18111).

**Test**
Test modified in StreamsBuilderTest.java covering the corrected behaviour.
