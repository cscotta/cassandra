**Commit**: 5a2dcf8fd0 · **Ticket**: KAFKA-20173
**Subsystem**: streams/src
**Category**: Wrong serialization / deserialization

**Root cause**
Ensures that all Metered Session-stores (plain and headers) pass headers
into de/serializers. Reviewers: Uladzislau Blok <blokv75@gmail.com>, TengYao Chi
 <frankvicky@apache.org>

**Before/After**
```java
// Before
import org.apache.kafka.common.header.internals.RecordHeaders;
// TODO: replace with new method in follow-up PR of KIP-1271
@SuppressWarnings("deprecation")
    MeteredSessionStore(final SessionStore<Bytes, byte[]> inner,
                        final String metricsScope,
                        final Serde<K> keySerde,
                        final Serde<V> valueSerde,
                        final Time time) {
    public void init(final StateStoreContext stateStoreContext,
                     final StateStore root) {
    public boolean setFlushListener(final CacheFlushListener<Windowed<K>, V> listener,
                                    final boolean sendOldValues) {

// After
    MeteredSessionStore(
        final SessionStore<Bytes, byte[]> inner,
        final String metricsScope,
        final Serde<K> keySerde,
        final Serde<V> valueSerde,
        final Time time
    ) {
    public void init(
        final StateStoreContext stateStoreContext,
        final StateStore root
    ) {
    public boolean setFlushListener(
```

**How to spot**
When reviewing streams/src code, look for wrong serialization / deserialization patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-20173: ensure metered session-stores pass headers correctly (#21957).

**Test**
Test modified in MeteredSessionStoreTest.java covering the corrected behaviour.
