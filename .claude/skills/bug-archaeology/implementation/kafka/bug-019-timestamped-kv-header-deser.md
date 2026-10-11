**Commit**: c1f901cdb2 · **Ticket**: KAFKA-20132
**Subsystem**: streams — MeteredTimestampedKeyValueStoreWithHeaders
**Category**: Wrong serialization / deserialization

**Root cause**
`MeteredTimestampedKeyValueStoreWithHeaders` inherited iterator-returning methods (`range`, `reverseRange`, `all`, `reverseAll`, `prefixScan`) from `MeteredKeyValueStore`. Those inherited methods called `deserializeKey(rawKey)` → `serdes.keyFrom(rawKey, internalContext.headers())`, using the *current processing context's* headers instead of each record's own headers. For key deserializers that require record-level headers (the entire point of the headers-store), deserialization would fail or produce wrong keys. Additionally, the inherited `peekNextKey()` passed empty `RecordHeaders`.

**Before/After**
```java
// Before — inherited from MeteredKeyValueStore, uses context headers
public KeyValueIterator<K, V> all() {
    // calls deserializeKey which reads from internalContext.headers()
}

// After — overridden in MeteredTimestampedKeyValueStoreWithHeaders
@Override
public KeyValueIterator<K, ValueTimestampHeaders<V>> all() {
    return new MeteredValueTimestampHeadersIterator(wrapped().all(), allSensor);
    // iterator deserializes value first to extract per-record headers, then deserializes key
}
```

**How to spot**
Subclasses that wrap a store with header-based key serialization must override *every* iterator method from the parent, not just `get`/`put`. The parent class uses a context-level header source; the header-aware subclass must extract headers from each record's value before deserializing that record's key. A missing override silently falls back to wrong headers.

**Test**
`MeteredTimestampedKeyValueStoreWithHeadersTest` — tests for `range`, `reverseRange`, `all`, `reverseAll`, `prefixScan`, and `peekNextKey` verify that keys are deserialized using the correct per-record headers from the stored value.
