**Commit**: cc90f0bc3b · **Ticket**: KAFKA-20173
**Subsystem**: streams/src
**Category**: Logic error in condition

**Root cause**
Updates the metered ks-stores layer to pass the context headers into
serdes. Simplifies the code with some refactoring. Reviewers: Uladzislau Blok <blokv75@gmail.com>, Alieh Saeedi
<asaeedi@confluent.io>, Bill Bejeck <bill@confluent.io>, TengYao Chi
<frankvicky@apache.org>

**Before/After**
```java
// Before
import org.apache.kafka.common.header.internals.RecordHeaders;
import static org.apache.kafka.streams.state.internals.Utils.keyBytes;
// TODO: replace with new method in follow-up PR of KIP-1271
@SuppressWarnings("deprecation")
    MeteredKeyValueStore(final KeyValueStore<Bytes, byte[]> inner,
                         final String metricsScope,
                         final Time time,
                         final Serde<K> keySerde,
                         final Serde<V> valueSerde) {
    public void init(final StateStoreContext stateStoreContext,
                     final StateStore root) {
        internalContext = stateStoreContext instanceof InternalProcessorContext ? (InternalProcessorContext<?, ?>) stateStoreContext : null;

// After
    MeteredKeyValueStore(
        final KeyValueStore<Bytes, byte[]> inner,
        final String metricsScope,
        final Time time,
        final Serde<K> keySerde,
        final Serde<V> valueSerde
    ) {
    public void init(final StateStoreContext stateStoreContext, final StateStore root) {
        internalContext = (InternalProcessorContext<?, ?>) stateStoreContext;
        StateStoreMetrics.addNumOpenIteratorsGauge(
            taskId.toString(),
            metricsScope,
```

**How to spot**
When reviewing streams/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-20173: metered layer of kv-stores needs to pass headers (#21684).

**Test**
Test modified in KeyValueStoreTestDriver.java covering the corrected behaviour.
