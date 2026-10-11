# Bug 2480: CLI SET command ignores column family default validator, uses raw bytes instead

**Commit**: f0b14509e1 · **Ticket**: CASSANDRA-3553

## Detection Rule

When a fallback code path returns a value with no type conversion (`ByteBufferUtil.bytes(columnValue)` i.e., raw UTF-8 bytes) instead of using the declared column family validator, values are silently stored in the wrong encoding.

## Code Smell

A method that looks up per-column validators in a loop and falls through to `return ByteBufferUtil.bytes(columnValue)` as a default — ignoring the column family's `default_validation_class` entirely.

## Tags

- scope: cassandra
- category: wrong-filter-result
- code-pattern: missing-default-validator-fallback
- severity: silent-wrong-result
- subsystem: tools-clients

## Root Cause

`CliClient.columnValueAsBytes()` iterated over per-column metadata to find a validator for the given column name. When no per-column definition matched, the code fell back to `ByteBufferUtil.bytes(columnValue)` (raw string bytes) instead of using the CF's `default_validation_class`. This meant SET commands on columns without explicit metadata would silently store the value as raw UTF-8, incompatible with the declared default type.

## Example

```java
// BEFORE
// if no validation were set returning simple .getBytes()
return ByteBufferUtil.bytes(columnValue);
```

```java
// AFTER
AbstractType defaultValidator = getFormatType(columnFamilyDef.default_validation_class);
// ... per-column loop ...
return defaultValidator.fromString(columnValue);
```
