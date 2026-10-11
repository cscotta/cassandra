**Commit**: 9834b5189c · **Ticket**: KAFKA-20134
**Subsystem**: streams — MeteredTimestampedWindowStoreWithHeaders
**Category**: Wrong serialization / deserialization

**Root cause**
`MeteredTimestampedWindowStoreWithHeaders` inherited `fetchAll`, `backwardFetchAll`, `all`, `backwardAll`, and the key-range `fetch`/`backwardFetch` methods from `MeteredWindowStore`. The base class iterators used the *deprecated* `serdes::keyFrom(rawKey)` overload, which provided empty headers. For header-dependent key deserializers this either threw or returned the wrong key. The fix overrides each method with a custom iterator (`MeteredTimestampedWindowStoreWithHeadersKeyValueIterator`) that deserializes the value as `ValueTimestampHeaders` first to extract per-record headers, then deserializes the key with those headers. `peekNextKey()` is also fixed to use a `cachedNext` pattern instead of calling the base `peekNextKey()`.

**Before/After**
```java
// Before — inherited, uses deprecated keyFrom without headers
// serdes.keyFrom(rawKey)  ← empty headers, wrong for header-dependent keys

// After — overridden iterator
@Override
public KeyValueIterator<Windowed<K>, ValueTimestampHeaders<V>> all() {
    return new MeteredTimestampedWindowStoreWithHeadersKeyValueIterator(wrapped().all());
    // .next() does:
    //   vth = serdes.valueFrom(raw, new RecordHeaders())
    //   headers = vth != null ? vth.headers() : new RecordHeaders()
    //   key = serdes.keyFrom(raw, headers)
}
```

**How to spot**
Third in the same `Metered*WithHeaders` pattern (see also KAFKA-20132, KAFKA-20158). Any window/session store with header-based keys must override all iterator-returning methods from the parent. The deprecated `serdes.keyFrom(byte[])` call (no headers argument) is the diagnostic signal — always replace with the two-argument overload using per-record headers.

**Test**
`MeteredTimestampedWindowStoreWithHeadersTest` — tests added for `fetchAll`, `backwardFetchAll`, `all`, `backwardAll`, and `peekNextKey` with a header-dependent key deserializer, asserting that the correct key is returned.
