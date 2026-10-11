**Commit**: 8f77ee6f78 · **Ticket**: KAFKA-20158
**Subsystem**: streams/src
**Category**: Logic error in condition

**Root cause**
This is a follow-up PR for RocksDBSessionStore with headers for
KIP-1271, adding session store with headers infrastructure classes
required for IQv2 support including SessionToHeadersStoreAdapter,
SessionStoreBuilderWithHeaders, and related iterators. These classes
enable the query delegation chain needed for Interactive Queries v2 to
work through session stores with headers, returning plain values without
exposing headers. Reviewers: Matthias Sax <matthias@confluent.io>

**Before/After**
```java
// Before
// (no removals)

// After
import org.apache.kafka.streams.state.internals.SessionStoreBuilderWithHeaders;
    /**
     * Creates a {@link StoreBuilder} that can be used to build a {@link SessionStoreWithHeaders}.
     *
     * @param supplier      a {@link SessionBytesStoreSupplier} (cannot be {@code null})
     * @param keySerde      the key serde to use
     * @param valueSerde    the value serde to use
     * @param <K>           key type
     * @param <V>           value type
     * @return an instance of {@link StoreBuilder} than can build a {@link SessionStoreWithHeaders}
     */
    public static <K, V> StoreBuilder<SessionStoreWithHeaders<K, V>> sessionStoreBuilderWithHeaders(final SessionBytesStoreSupplier supplier,
```

**How to spot**
When reviewing streams/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-20158: adding sessionstore with headers adapters and iterator facade (#21680).

**Test**
Test modified in SessionToHeadersIteratorAdapterTest.java covering the corrected behaviour.
