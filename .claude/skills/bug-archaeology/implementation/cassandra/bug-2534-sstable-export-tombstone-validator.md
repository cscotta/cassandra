# Bug 2534: SSTableExport applies value validator to tombstone column bytes

**Commit**: 03f24a9a32 · **Ticket**: CASSANDRA-3357

## Detection Rule
When serializing a column value for export, check whether the column is a `DeletedColumn` first; tombstone values store a raw deletion time as bytes and must not be passed through the column family's value validator for display.

## Code Smell
`serializedColumn.add(validator.getString(value))` called unconditionally — for a `DeletedColumn`, `value` contains a 4-byte big-endian deletion timestamp, not a user-value of the declared type; passing it through the validator throws a `MarshalException` or produces a corrupt hex string, and on import the same bytes are re-validated and rejected.

## Tags
- scope: cassandra
- category: wrong-serialization
- code-pattern: type-validator-applied-to-tombstone
- severity: data-loss
- subsystem: tools-clients

## Root Cause
`SSTableExport` serialized every column's value through `cfMetaData.getValueValidator(name).getString(value)`. Tombstone columns use the value bytes to store the local deletion time (a 4-byte integer), not a user value. Passing these bytes through (for example) a `UTF8Type` validator throws an exception or produces garbage JSON. `SSTableImport` then tries to parse that output and fails. The fix exports tombstone values as raw hex and imports them back as raw hex, bypassing the validator.

## Example
```java
// BEFORE
serializedColumn.add(validator.getString(value)); // throws for tombstones
```
```java
// AFTER
if (column instanceof DeletedColumn)
    serializedColumn.add(ByteBufferUtil.bytesToHex(value));
else
    serializedColumn.add(validator.getString(value));
```
