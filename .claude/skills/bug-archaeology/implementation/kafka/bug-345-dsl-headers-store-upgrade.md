## Commit
e02284998b

## Ticket
KAFKA-20194

## Subsystem
Kafka Streams / DSL Processors

## Category
Wrong constant / default value

## Root cause
Kafka Streams DSL processors (`KStreamAggregate`, `KStreamReduce`, `KStreamJoinWindow`, `KStreamKStreamJoin`, etc.) were backed by `TimestampedWindowStore<K, VAgg>` instead of the new `TimestampedWindowStoreWithHeaders<K, VAgg>`. Headers were not stored alongside values, so header-propagation through stream processing was silently dropped. The fix changes all DSL store builders to `Stores.timestampedWindowStoreWithHeadersBuilder(...)` and updates processor generics.

## Before
```java
protected TimestampedWindowStore<KIn, VAgg> windowStore;
// join window backed by Stores.windowStoreBuilder(...)
```

## After
```java
protected TimestampedWindowStoreWithHeaders<KIn, VAgg> windowStore;
// join window backed by Stores.timestampedWindowStoreWithHeadersBuilder(...)
// KStreamJoinWindow stores ValueTimestampHeaders.make(value, timestamp, headers)
```

## How to spot
Any DSL processor that stores intermediate aggregates without using the `WithHeaders` store variant will silently drop headers on the stored value. Search for `TimestampedWindowStore` fields in DSL processors that should be `TimestampedWindowStoreWithHeaders`.

## Test
`IQv2StoreIntegrationTest`, `QueryableStateIntegrationTest` verify headers are preserved end-to-end.
