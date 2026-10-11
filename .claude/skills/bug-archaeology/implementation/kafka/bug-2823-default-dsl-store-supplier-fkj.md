**Commit**: a0bfc64470 · **Ticket**: KAFKA-15774
**Subsystem**: streams/src
**Category**: Logic error in condition

**Root cause**
Foreign key joins have an additional "internal" state store used for subscriptions, which is not exposed for configuration via Materialized or StoreBuilder which means there is no way to plug in a different store implementation via the DSL operator. However, we should respect the configured default dsl store supplier if one is configured, to allow these stores to be customized and conform to the store type selection logic used for other DSL operator stores

Reviewers: Anna Sophie Blee-Goldman <ableegoldman@apache.org>

**Before/After**
```java
// Before
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.state.Stores;
import org.apache.kafka.streams.state.TimestampedKeyValueStore;
        final StoreBuilder<TimestampedKeyValueStore<Bytes, SubscriptionWrapper<K>>> subscriptionStore =
            Stores.timestampedKeyValueStoreBuilder(
                Stores.persistentTimestampedKeyValueStore(
                    renamed.suffixWithOrElseGet("-subscription-store", builder, FK_JOIN_STATE_STORE_NAME)
                ),
                new Serdes.BytesSerde(),
                subscriptionWrapperSerde
            );
        builder.addStateStore(new StoreBuilderWrapper(subscriptionStore));

// After
        final String subscriptionStoreName = renamed
            .suffixWithOrElseGet("-subscription-store", builder, FK_JOIN_STATE_STORE_NAME);
        builder.addStateStore(
            new SubscriptionStoreFactory<>(subscriptionStoreName, subscriptionWrapperSerde));
        final String subscriptionReceiveName = renamed.suffixWithOrElseGet(
            "-subscription-receive", builder, SUBSCRIPTION_PROCESSOR);
                subscriptionReceiveName,
                    new SubscriptionReceiveProcessorSupplier<>(subscriptionStoreName, combinedKeySchema),
                    subscriptionReceiveName),
                new String[]{subscriptionStoreName}
        final String foreignTableJoinName = renamed
            .suffixWithOrElseGet("-foreign-join-subscription", builder, SUBSCRIPTION_PROCESSOR);
```

**How to spot**
When reviewing streams/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-15774: use the default dsl store supplier for fkj subscriptions (#16380).

**Test**
Test modified in ForeignTableJoinProcessorSupplierTests.java covering the corrected behaviour.
