# Bug 0539: InternalRecordWrapper crashes or produces wrong results for TIMESTAMP_NANO type

**Commit**: 1dce77c788a761f74e7ff8a4f5ce2d0a75b06359 · **Ticket**: #14974

## Detection Rule

When a type-dispatch switch or if-chain handles primitive/extended types, verify that every type variant defined in the type enum or hierarchy has a corresponding case; a missing variant will fall through to an unrelated branch or throw a runtime exception.

## Code Smell

A `switch` or series of `if/else` blocks over an Iceberg `TypeID` enum that has a `case TIMESTAMP` but no `case TIMESTAMP_NANO`, leaving the nanosecond variant unhandled.

## Tags

- scope: project-specific
- category: silent-skip
- code-pattern: sentinel-not-handled
- severity: crash
- subsystem: data/internal-record-wrapper

## Root Cause

`InternalRecordWrapper.converter()` dispatches on `type.typeId()` to produce a Java-object-to-internal-representation converter. The `TIMESTAMP_NANO` type ID was added to Iceberg's type system but was never added as a case in the converter chain. Any attempt to wrap a record containing a nanosecond-precision timestamp would fall through to the `FIXED` case (which wraps bytes) or throw a `ClassCastException`, depending on evaluation order, producing garbage output or a runtime crash instead of correct nanosecond longs.

## Example

```java
// BEFORE
switch (type.typeId()) {
  case TIMESTAMP:
    if (((Types.TimestampType) type).shouldAdjustToUTC()) {
      return ts -> DateTimeUtil.microsFromTimestamptz((OffsetDateTime) ts);
    } else {
      return ts -> DateTimeUtil.microsFromTimestamp((LocalDateTime) ts);
    }
  case FIXED:  // TIMESTAMP_NANO falls through here — ClassCastException at runtime
    return bytes -> ByteBuffer.wrap((byte[]) bytes);
```

```java
// AFTER
switch (type.typeId()) {
  case TIMESTAMP:
    if (((Types.TimestampType) type).shouldAdjustToUTC()) {
      return ts -> DateTimeUtil.microsFromTimestamptz((OffsetDateTime) ts);
    } else {
      return ts -> DateTimeUtil.microsFromTimestamp((LocalDateTime) ts);
    }
  case TIMESTAMP_NANO:
    if (((Types.TimestampNanoType) type).shouldAdjustToUTC()) {
      return ts -> DateTimeUtil.nanosFromTimestamptz((OffsetDateTime) ts);
    } else {
      return ts -> DateTimeUtil.nanosFromTimestamp((LocalDateTime) ts);
    }
  case FIXED:
    return bytes -> ByteBuffer.wrap((byte[]) bytes);
```
