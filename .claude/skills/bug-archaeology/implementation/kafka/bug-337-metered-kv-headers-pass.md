## Commit
e952ccd52b

## Ticket
KAFKA-20173

## Subsystem
Kafka Streams / State Store Internals

## Category
Logic error in condition

## Root cause
`MeteredKeyValueStore` and related metered stores did not pass `Headers` to serializers/deserializers in several code paths: `range()`, `reverseRange()`, `all()`, `reverseAll()`, `prefixScan()`, and query paths. This caused headers-aware serdes to receive an empty header set, producing incorrect serialized keys. The fix renames `MeteredKeyValueTimestampedIterator` to `MeteredKeyValueStoreIterator` and removes the separate timestamped iterator class, unifying the deserialization path to always pass `internalContext.headers()`.

## Before
```java
// prefixScan / range / all
return new MeteredKeyValueIterator(wrapped().range(...), rangeSensor);
// MeteredKeyValueTimestampedIterator used custom valueDeserializer Function
```

## After
```java
// Single MeteredKeyValueStoreIterator used everywhere
return new MeteredKeyValueStoreIterator(wrapped().range(...), rangeSensor);
// deserializeKey always calls serdes.keyFrom(rawKey, internalContext.headers())
```

## How to spot
When a class has two nearly identical iterator inner classes that differ only in deserialization logic, the distinction is likely a bug—one path is silently dropping headers. Consolidate into a single class that always uses the context headers.

## Test
`MeteredKeyValueStoreTest` verifies headers are passed through on all iterator paths.
