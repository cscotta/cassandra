**Commit**: fb36f97d96 · **Ticket**: KAFKA-20304
**Subsystem**: streams/src
**Category**: Logic error in condition

**Root cause**
This PR
- adds all `ReadOnly*Facade` classes required to wrap headers store for
backward compatibility
- refactors `ReadOnly*Facade` classes to use generic infrastructure with
converter functions

Reviewers: Matthias J. Sax <matthias@confluent.io>

**Before/After**
```java
// Before
import org.apache.kafka.streams.state.ValueAndTimestamp;
import static org.apache.kafka.streams.state.ValueAndTimestamp.getValueOrNull;
public class KeyValueIteratorFacade<K, V> implements KeyValueIterator<K, V> {
    private final KeyValueIterator<K, ValueAndTimestamp<V>> innerIterator;

    public KeyValueIteratorFacade(final KeyValueIterator<K, ValueAndTimestamp<V>> iterator) {
        innerIterator = iterator;
    public boolean hasNext() {
        return innerIterator.hasNext();
    public KeyValue<K, V> next() {
        final KeyValue<K, ValueAndTimestamp<V>> innerKeyValue = innerIterator.next();
        return KeyValue.pair(innerKeyValue.key, getValueOrNull(innerKeyValue.value));

// After
import java.util.function.Function;
/**
 * Generic iterator facade that wraps a {@link KeyValueIterator} and converts values
 * using a provided converter function.
 *
 * @param <K> key type
 * @param <InV> input value type (from inner iterator)
 * @param <OutV> output value type (exposed by this facade)
 */
class GenericKeyValueIteratorFacade<K, InV, OutV> implements KeyValueIterator<K, OutV> {
    private final KeyValueIterator<K, InV> innerIterator;
    private final Function<InV, OutV> valueConverter;
```

**How to spot**
When reviewing streams/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-20304: implement all required readonly*facade classes for headers stores (#21743).

**Test**
Test modified in GlobalStateStoreProvider.java covering the corrected behaviour.
