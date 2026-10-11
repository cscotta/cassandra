## Commit
1ad463d9cc

## Ticket
KAFKA-20318

## Subsystem
Kafka Streams / RocksDB State Stores

## Category
Wrong constant / default value

## Root cause
`RocksDbTimeOrderedSessionBytesStoreSupplier.get()` with `withHeaders=true` instantiated `AbstractRocksDBTimeOrderedSegmentedBytesStore` using `KeyValueSegment` instead of `SessionSegmentWithHeaders`. The class was not generic over segment type, so all code paths used the same fixed segment class regardless of the `withHeaders` flag.

## Before
```java
// Always uses KeyValueSegment
return new RocksDBTimeOrderedKeyValueBytesStore(..., KeyValueSegment.class);
```

## After
```java
// Generic over <S extends Segment>; withHeaders=true uses SessionSegmentWithHeaders
return new RocksDBTimeOrderedKeyValueBytesStore<>(..., SessionSegmentWithHeaders.class);
```

## How to spot
Feature-flag parameters (`withHeaders`, `withTimestamps`) that control which type to instantiate should be checked at every construction point. If a class is not generic over the type it should conditionally use, the flag is effectively ignored.

## Test
`RocksDbTimeOrderedSessionBytesStoreSupplierTest` asserts the correct segment type is created based on the flag.
