**Commit**: be3ab8bdd5 · **Ticket**: KAFKA-14460
**Subsystem**: streams/src
**Category**: Logic error in condition

**Root cause**
As described in KAFKA-14460, one of the functional requirements of KeyValueStore is that "The returned iterator must not return null values" on methods which return iterator. This is not completely the case today for InMemoryKeyValueStore. To iterate over the store, we copy the keySet in order not to block access for other threads. However, entries that are removed from the store after initializing the iterator will be returned with null values by the iterator.

**Before/After**
```java
// Before
        final KeyValueIterator<Bytes, byte[]> storeIterator =
            new DelegatingPeekingKeyValueIterator<>(this.name(), wrapped().all());
        final KeyValueIterator<Bytes, byte[]> storeIterator =
            new DelegatingPeekingKeyValueIterator<>(this.name(), wrapped().reverseAll());
import org.apache.kafka.streams.errors.InvalidStateStoreException;
            throw new InvalidStateStoreException(String.format("Store %s has closed", storeName));
        return new DelegatingPeekingKeyValueIterator<>(
            name,
            new InMemoryKeyValueIterator(map.subMap(from, true, to, false).keySet(), true)
        );
        return new DelegatingPeekingKeyValueIterator<>(name, new InMemoryKeyValueIterator(rangeSet, forward));
        return new DelegatingPeekingKeyValueIterator<>(

// After
        final KeyValueIterator<Bytes, byte[]> storeIterator = wrapped().all();
        final KeyValueIterator<Bytes, byte[]> storeIterator = wrapped().reverseAll();
            throw new IllegalStateException(String.format("Iterator for store %s has already been closed.", storeName));
import java.util.NoSuchElementException;
        return new InMemoryKeyValueIterator(map.subMap(from, true, to, false).keySet(), true);
        return new InMemoryKeyValueIterator(rangeSet, forward);
        return new InMemoryKeyValueIterator(map.keySet(), false);
        private Bytes currentKey;
        private Boolean iteratorOpen = true;
```

**How to spot**
When reviewing streams/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-14460: skip removed entries from in-memory keyvalueiterator (#16505).

**Test**
Test modified in InMemoryKeyValueStoreTest.java covering the corrected behaviour.
