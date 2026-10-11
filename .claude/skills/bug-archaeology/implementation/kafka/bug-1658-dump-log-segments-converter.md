## Commit

- Hash: 273719227e
- Tickets: KAFKA-18457
- Subject: KAFKA-18457: Use generated CoordinatorRecordJsonConverters in DumpLogSegments

## Subsystem

tools / DumpLogSegments / coordinator record serialization

## Category

Silent skip on non-match

## Root cause

`DumpLogSegments`'s `OffsetsMessageParser` and `ShareGroupStateMessageParser` contained manual per-type switch statements to serialize coordinator records to JSON. When a new record type was added to the coordinator protocol, developers had to remember to update both parsers independently. Missing cases produced silent `null` / empty output rather than an error. Additionally, the `ShareGroupStateMessageParser` used `"type"` as the JSON field name for the schema version instead of `"version"`, producing incorrect output.

## Before

```scala
// Manual switch — new record types silently produce null output
recordType match {
  case OFFSET_COMMIT_KEY => ...
  case GROUP_METADATA_KEY => ...
  // missing cases fall through silently
}
```

## After

```scala
// Generated dispatcher handles all current and future types
CoordinatorRecordJsonConverters.writeRecordKeyAsJson(recordType, key)
CoordinatorRecordJsonConverters.writeRecordValueAsJson(recordType, value)
```

Also fixed: JSON field renamed from `"type"` to `"version"` in `ShareGroupStateMessageParser`.

## How to spot

Hand-maintained switch/match blocks over record type enums that must be kept in sync with a separately generated or evolving protocol. Any such block is a maintenance bug waiting to happen — a code generator should own the dispatch.

## Test

`DumpLogSegmentsTest` extended with round-trip assertions for coordinator record types.
