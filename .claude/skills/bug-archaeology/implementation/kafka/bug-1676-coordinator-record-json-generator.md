## Commit

- Hash: 6cc059dced
- Tickets: KAFKA-18304
- Subject: KAFKA-18304: Add CoordinatorRecordJsonConvertersGenerator code generator

## Subsystem

generator / coordinator record serialization

## Category

Silent skip on non-match

## Root cause

JSON conversion of coordinator log records was done via hand-maintained switch blocks in multiple classes (e.g. `DumpLogSegments`, `GroupCoordinatorRecordSerde`). Every new record type required updating each switch independently; a missed case would either throw at runtime or produce empty/wrong JSON output. The fix introduces a code generator (`CoordinatorRecordJsonConvertersGenerator`) that reads the coordinator record schema and emits a `CoordinatorRecordJsonConverters` class with exhaustive, compile-time-complete `writeRecordKeyAsJson`, `writeRecordValueAsJson`, `readRecordKeyFromJson`, and `readRecordValueFromJson` methods.

## Before

Dispatch logic duplicated across multiple hand-maintained switch blocks; new types silently skipped on non-match.

## After

`CoordinatorRecordJsonConvertersGenerator` generates `CoordinatorRecordJsonConverters.java` at build time; all callers delegate to it.

## How to spot

Multiple files independently maintaining switch-on-record-type logic that must stay in sync with a schema definition. The schema should drive a single code-generated dispatcher; duplication is the smell.

## Test

Generator test verifies output compiles and round-trips all current coordinator record types.
