**Commit**: a2efacd44e · **Ticket**: KAFKA-20307
**Subsystem**: streams/src
**Category**: Logic error in condition

**Root cause**
This PR adds backward compatibility support for the new headers-aware
state stores (TimestampedKeyValueStoreWithHeaders and
TimestampedWindowStoreWithHeaders) when queried through the
Interactive Queries v1 (IQv1) API. Users can now query headers-aware
stores using the existing QueryableStoreTypes API without any code
changes. Reviewers: Matthias J. Sax <matthias@confluent.io>

**Before/After**
```java
// Before
                        .getStore(STORE_NAME, kafkaStreams, QueryableStoreTypes.keyValueStore());
                        .getStore(STORE_NAME, kafkaStreams, QueryableStoreTypes.keyValueStore());
                        .getStore(STORE_NAME, kafkaStreams, QueryableStoreTypes.keyValueStore());
    private <K, V> void verifyLegacyValuesWithEmptyHeaders(final K key,
                                                           final V value) throws Exception {
        TestUtils.waitForCondition(
            () -> {
                try {
                    final ReadOnlyKeyValueStore<K, ValueTimestampHeaders<V>> store = IntegrationTestUtils
                        .getStore(STORE_NAME, kafkaStreams, QueryableStoreTypes.keyValueStore());

                    if (store == null)

// After
import org.apache.kafka.streams.state.QueryableStoreType;
import org.apache.kafka.streams.state.internals.CompositeReadOnlyKeyValueStore;
import org.apache.kafka.streams.state.internals.CompositeReadOnlyWindowStore;
import org.apache.kafka.streams.state.internals.StateStoreProvider;
                        .getStore(STORE_NAME, kafkaStreams, new TimestampedKeyValueStoreWithHeadersType<>());
                        .getStore(STORE_NAME, kafkaStreams, new TimestampedKeyValueStoreWithHeadersType<>());
                        .getStore(STORE_NAME, kafkaStreams, new TimestampedKeyValueStoreWithHeadersType<>());
                    IntegrationTestUtils.getStore(WINDOW_STORE_NAME, kafkaStreams, new TimestampedWindowStoreWithHeadersType<>());
```

**How to spot**
When reviewing streams/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-20307: add interactive queries (iqv1) support for headers-aware stores (#21744).

**Test**
Test modified in HeadersStoreUpgradeIntegrationTest.java covering the corrected behaviour.
