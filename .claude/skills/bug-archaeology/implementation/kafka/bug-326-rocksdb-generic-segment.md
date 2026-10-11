## Commit
a6fd96c36f

## Ticket
KAFKA-20317

## Subsystem
Kafka Streams / RocksDB State Stores

## Category
Logic error in condition

## Root cause
`AbstractRocksDBTimeOrderedSegmentedBytesStore` was fixed to a single segment type (`KeyValueSegment`). Making it generic over `<S extends Segment>` allows callers to specify `RocksDBMigratingWindowStoreWithHeaders` or other segment implementations. The `convertToHeaderFormat` Javadoc was also updated to clarify its general-purpose nature.

## Before
```java
public class AbstractRocksDBTimeOrderedSegmentedBytesStore {
    // hardcoded KeyValueSegment internally
}
```

## After
```java
public class AbstractRocksDBTimeOrderedSegmentedBytesStore<S extends Segment> {
    // S used for segment creation
}
```

## How to spot
When a class needs to support multiple segment implementations selected at construction time, parameterize it generically. Hardcoded segment types prevent extension without subclassing.

## Test
`AbstractRocksDBTimeOrderedSegmentedBytesStoreTest` with both `KeyValueSegment` and `RocksDBMigratingWindowStoreWithHeaders` parameterizations.
