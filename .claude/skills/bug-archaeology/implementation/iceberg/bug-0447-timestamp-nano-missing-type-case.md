# Bug 0447: TIMESTAMP_NANO values silently passed through unconverted in generic data model

**Commit**: 03ed4ba9af4e47d32bdb22b7e3d033eb2a4b2c83 · **Ticket**: #15099

## Detection Rule

When a `switch` or `if-else` chain converts internal Iceberg type IDs to generic Java representations, verify that every distinct type ID variant (e.g. `TIMESTAMP` vs `TIMESTAMP_NANO`, `DECIMAL` variants) has an explicit case, because a missing case silently falls through to the default return that passes the raw internal value to callers expecting a converted object.

## Code Smell

A `switch (type.typeId())` block in a type-conversion utility where `TIMESTAMP` is handled but `TIMESTAMP_NANO` has no case, so the raw `Long` nanosecond value is returned as-is instead of being converted to `LocalDateTime` or `OffsetDateTime`.

## Tags

- scope: project-specific
- category: silent-skip
- code-pattern: missing-type-variant-case
- severity: silent-wrong-result
- subsystem: core/data-model-conversion

## Root Cause

`GenericDataUtil.internalToGeneric` was extended to support `TIMESTAMP` but the sibling type `TIMESTAMP_NANO` (added later) was never added to the switch statement. The default path returns the raw `Long` nanosecond value, so callers that expect a `LocalDateTime` or `OffsetDateTime` receive an unconverted number, producing silent data corruption when reading nanosecond-precision timestamps through the generic data model.

## Example

```java
// BEFORE
case TIMESTAMP:
  if (((Types.TimestampType) type).shouldAdjustToUTC()) {
    return DateTimeUtil.timestamptzFromMicros((Long) value);
  } else {
    return DateTimeUtil.timestampFromMicros((Long) value);
  }
case FIXED:
  return ByteBuffers.toByteArray((ByteBuffer) value);
// TIMESTAMP_NANO falls to default: returns raw Long
```

```java
// AFTER
case TIMESTAMP:
  if (((Types.TimestampType) type).shouldAdjustToUTC()) {
    return DateTimeUtil.timestamptzFromMicros((Long) value);
  } else {
    return DateTimeUtil.timestampFromMicros((Long) value);
  }
case TIMESTAMP_NANO:
  if (((Types.TimestampNanoType) type).shouldAdjustToUTC()) {
    return DateTimeUtil.timestamptzFromNanos((Long) value);
  } else {
    return DateTimeUtil.timestampFromNanos((Long) value);
  }
case FIXED:
  return ByteBuffers.toByteArray((ByteBuffer) value);
```
