**Commit**: 9f15f269d5 · **Ticket**: KAFKA-20158
**Subsystem**: streams — MeteredSessionStoreWithHeaders
**Category**: Wrong serialization / deserialization

**Root cause**
`MeteredSessionStoreWithHeaders` inherited all `fetch`/`backwardFetch`/`findSessions`/`backwardFindSessions` iterator methods from `MeteredSessionStore`. Those inherited iterators called `serdes.keyFrom(bytes, new RecordHeaders())`, providing *empty* headers when deserializing each record's key. For key serializers that embed header-based context in the key, this caused deserialization failures or silently wrong keys. The fix overrides every iterator method with a custom inner iterator that deserializes the *value* first (as `AggregationWithHeaders`) to extract the record-level headers, then uses those headers to deserialize the key.

**Before/After**
```java
// Before — inherited, uses empty headers for key deserialization
// serdes.keyFrom(next.key.key().get(), new RecordHeaders())  ← always empty

// After — overridden in MeteredSessionStoreWithHeaders
@Override
public KeyValueIterator<Windowed<K>, AggregationWithHeaders<AGG>> fetch(K key) {
    return new MeteredSessionStoreWithHeadersIterator(wrapped().fetch(...));
    // MeteredSessionStoreWithHeadersIterator.next() does:
    //   value = serdes.valueFrom(...)           // extract AggregationWithHeaders
    //   headers = value.headers()               // per-record headers
    //   key = serdes.keyFrom(bytes, headers)    // correct deserialization
}
```

**How to spot**
Same pattern as KAFKA-20132 (commit c1f901cdb2) — any `Metered*WithHeaders` store that inherits iteration methods from a base class that passes `new RecordHeaders()` to key deserialization is broken. Audit every `Metered*WithHeaders` class for missing iterator overrides. The fix template is consistent: always deserialize value first, extract headers, then deserialize key.

**Test**
`MeteredSessionStoreWithHeadersTest` — new tests for each overridden iterator method verify that the key is deserialized using the headers from the associated `AggregationWithHeaders` value.
