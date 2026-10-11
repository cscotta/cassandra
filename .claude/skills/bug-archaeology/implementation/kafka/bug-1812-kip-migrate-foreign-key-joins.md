**Commit**: bac8928521 · **Ticket**: KAFKA-18026
**Subsystem**: streams/src
**Category**: Logic error in condition

**Root cause**
Convert FKJ processors to implementing the #stores method

Reviewers: Guozhang Wang <guozhang.wang.us@gmail.com>

**Before/After**
```java
// Before
        builder.addStateStore(
            new SubscriptionStoreFactory<>(subscriptionStoreName, subscriptionWrapperSerde));
                    new SubscriptionReceiveProcessorSupplier<>(subscriptionStoreName, combinedKeySchema),
        final StatefulProcessorNode<KO, Change<VO>> foreignTableJoinNode = new ForeignTableJoinNode<>(
                new ForeignTableJoinProcessorSupplier<>(subscriptionStoreName, combinedKeySchema),
            ),
            new String[]{subscriptionStoreName}
    private final String storeName;
    public ForeignTableJoinProcessorSupplier(
        final String storeName,
        final CombinedKeySchema<KO, K> keySchema) {


// After
        final StoreFactory subscriptionStoreFactory =
            new SubscriptionStoreFactory<>(subscriptionStoreName, subscriptionWrapperSerde);
                    new SubscriptionReceiveProcessorSupplier<>(subscriptionStoreFactory, combinedKeySchema),
        final ProcessorGraphNode<KO, Change<VO>> foreignTableJoinNode = new ForeignTableJoinNode<>(
                new ForeignTableJoinProcessorSupplier<>(subscriptionStoreFactory, combinedKeySchema),
            )
import org.apache.kafka.streams.processor.internals.StoreFactory;
import org.apache.kafka.streams.processor.internals.StoreFactory.FactoryWrappingStoreBuilder;
import org.apache.kafka.streams.state.StoreBuilder;
import java.util.Collections;
import java.util.Set;
    private final StoreFactory subscriptionStoreFactory;
```

**How to spot**
When reviewing streams/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-18026: kip-1112, migrate foreign-key joins to use processersupplier#stores  (#18194).

**Test**
Test modified in StreamsBuilderTest.java covering the corrected behaviour.
