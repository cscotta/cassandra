**Commit**: 53629a28ac · **Ticket**: KAFKA-20307
**Subsystem**: streams/src
**Category**: Logic error in condition

**Root cause**
We previously only added support to query headers stores as plain- or
ts-kv-stores. This PR exposes headers-store with their native type. Reviewers: Bill Bejeck <bbejeck@apache.org>

**Before/After**
```java
// Before
import org.apache.kafka.streams.state.QueryableStoreType;
import org.apache.kafka.streams.state.internals.CompositeReadOnlyKeyValueStore;
import org.apache.kafka.streams.state.internals.CompositeReadOnlyWindowStore;
import org.apache.kafka.streams.state.internals.StateStoreProvider;
import java.util.concurrent.atomic.AtomicReference;
        streamsConfiguration.put(StreamsConfig.DEFAULT_KEY_SERDE_CLASS_CONFIG, Serdes.String().getClass());
        streamsConfiguration.put(StreamsConfig.DEFAULT_VALUE_SERDE_CLASS_CONFIG, Serdes.String().getClass());
                        .getStore(STORE_NAME, kafkaStreams, new TimestampedKeyValueStoreWithHeadersType<>());
                        .getStore(STORE_NAME, kafkaStreams, new TimestampedKeyValueStoreWithHeadersType<>());
                        .getStore(STORE_NAME, kafkaStreams, new TimestampedKeyValueStoreWithHeadersType<>());
                    IntegrationTestUtils.getStore(WINDOW_STORE_NAME, kafkaStreams, new TimestampedWindowStoreWithHeadersType<>());
                    IntegrationTestUtils.getStore(WINDOW_STORE_NAME, kafkaStreams, new TimestampedWindowStoreWithHeadersType<>());

// After
        streamsConfiguration.put(StreamsConfig.DEFAULT_KEY_SERDE_CLASS_CONFIG, Serdes.StringSerde.class);
        streamsConfiguration.put(StreamsConfig.DEFAULT_VALUE_SERDE_CLASS_CONFIG, Serdes.StringSerde.class);
                        .getStore(STORE_NAME, kafkaStreams, QueryableStoreTypes.timestampedKeyValueStoreWithHeaders());
                        .getStore(STORE_NAME, kafkaStreams, QueryableStoreTypes.timestampedKeyValueStoreWithHeaders());
                        .getStore(STORE_NAME, kafkaStreams, QueryableStoreTypes.timestampedKeyValueStoreWithHeaders());
                    IntegrationTestUtils.getStore(WINDOW_STORE_NAME, kafkaStreams, QueryableStoreTypes.timestampedWindowStoreWithHeaders());
                    IntegrationTestUtils.getStore(WINDOW_STORE_NAME, kafkaStreams, QueryableStoreTypes.timestampedWindowStoreWithHeaders());
```

**How to spot**
When reviewing streams/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-20307: expose header-stores natively in iqv1 (#21992).

**Test**
Test modified in HeadersStoreUpgradeIntegrationTest.java covering the corrected behaviour.
