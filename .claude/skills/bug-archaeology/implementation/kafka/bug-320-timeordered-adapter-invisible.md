## Commit
bba9952184

## Ticket
KAFKA-20326

## Subsystem
Kafka Streams / State Store Internals

## Category
Silent skip on non-match

## Root cause
`TimeOrderedCachingWindowStore.getWrappedStore()` and `isTimeOrderedStore()` walked the wrapper chain using `instanceof WrappedStateStore` casts but `TimestampedToHeadersWindowStoreAdapter` did not extend `WrappedStateStore`. The chain walk silently stopped, so the adapter was invisible to store-type detection logic and DSL builders created the wrong underlying segment type.

## Before
```java
// TimestampedToHeadersWindowStoreAdapter NOT a WrappedStateStore
// -> chain walk stops; adapter invisible to type detection
```

## After
```java
// Added special-case in getWrappedStore() / isTimeOrderedStore()
if (store instanceof TimestampedToHeadersWindowStoreAdapter adapter) {
    store = adapter.inner();
}
// also: store field changed from private to package-private for test access
```

## How to spot
Any decorator/wrapper chain traversal using `instanceof WrappedStateStore` will silently terminate at any wrapper that does not implement that interface. Add tests that exercise the full chain and verify the correct leaf type is discovered.

## Test
`TimeOrderedCachingWindowStoreTest` asserts correct store detection with the adapter in the chain.
