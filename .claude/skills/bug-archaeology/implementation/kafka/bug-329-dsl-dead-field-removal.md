## Commit
56d562d4b3

## Ticket
KAFKA-20258

## Subsystem
Kafka Streams / DSL

## Category
State not cleaned up (dead code)

## Root cause
`DslKeyValueParams` had an `isTimestamped` field that was entirely redundant with `dslStoreFormat` (which already encodes whether the store is timestamped). The dead field was included in `equals()`, `hashCode()`, and `toString()`, causing incorrect equality results when two params differed only in the dead field's value.

## Before
```java
class DslKeyValueParams {
    private final boolean isTimestamped;  // dead field
    private final DslStoreSuppliers dslStoreFormat;
    // equals() included isTimestamped
}
```

## After
```java
class DslKeyValueParams {
    // isTimestamped removed entirely
    private final DslStoreSuppliers dslStoreFormat;
    // equals() only compares meaningful fields
}
```

## How to spot
Fields not set in any code path other than the constructor, or always set to the same value, are dead. Run a static analysis tool or search for fields that are only read in `equals()`/`hashCode()`/`toString()`.

## Test
`DslKeyValueParamsTest` verifies equals/hashCode after removal.
