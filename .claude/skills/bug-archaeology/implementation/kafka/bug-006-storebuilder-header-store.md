**Commit**: 914051f4c9 · **Ticket**: KAFKA-20194
**Subsystem**: streams — StoreBuilderWrapper / Stores API
**Category**: Wrong constant / default value

**Root cause**
`StoreBuilderWrapper` failed to recognize header-based store builders because the recognition logic did not check for the new header-store wrapper types introduced by KIP-1271. As a result, changelog topics for header-stores were created with incorrect configuration (missing the compact-and-delete setting needed by these stores). Additionally, the public `Stores` API had the wrong method names: `timestampedKeyValueStoreBuilderWithHeaders` and `sessionStoreBuilderWithHeaders` — the correct names per the KIP are `timestampedKeyValueStoreWithHeadersBuilder` and `sessionStoreWithHeadersBuilder`.

**Before/After**
```java
// Before (integration test / public API usage)
Stores.timestampedKeyValueStoreBuilderWithHeaders(...)   // wrong method name
Stores.sessionStoreBuilderWithHeaders(...)               // wrong method name

// After
Stores.timestampedKeyValueStoreWithHeadersBuilder(...)   // correct per KIP-1271
Stores.sessionStoreWithHeadersBuilder(...)               // correct per KIP-1271
```

**How to spot**
When a new store type is introduced, every place that branches on store type (builder wrappers, changelog topic configuration, etc.) must be updated. A missing branch silently falls through to wrong defaults. Search for all `instanceof` / type-switch checks in `StoreBuilderWrapper` and similar infrastructure whenever a new store variant is added.

**Test**
`HeadersStoreUpgradeIntegrationTest` — corrected all six call sites to use the renamed methods. A new `SessionsStoreWithHeadersTest` integration test exercises the session headers store end-to-end.
