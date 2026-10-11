# Bug 0303: Field statistics struct IDs off by one due to misaligned offset base

**Commit**: 17891bc4e300c49b3678117be3189864aaf3866b · **Ticket**: #15432

## Detection Rule

When an enum assigns integer offsets to struct field IDs and those offsets are also used as 0-based array positions, verify that the two usages are not conflated — offset-from-base-id and array-position-in-struct must be tracked separately.

## Code Smell

An enum of field-ID offsets where the first entry is 0 and the same values are passed to both `record.set(offset, value)` (0-based position) and `fieldId + offset` (1-based relative ID), plus a `size()` that returns one fewer than the actual number of fields.

## Tags

- scope: project-specific
- category: wrong-constant
- code-pattern: offset-position-conflation
- severity: silent-wrong-result
- subsystem: stats / schema / field-id allocation

## Root Cause

`FieldStatistic` enum assigned offsets 0–7, but the Iceberg spec requires field IDs for stats sub-fields to start at `baseFieldId + 1` (offset 1-based), not `baseFieldId + 0`. Callers that used the enum value as a 0-based struct position were correct for positional access, but callers computing field IDs via `fieldId + offset` needed a 1-based value. The two uses shared one integer field, so fixing the field-ID computation required introducing a separate `position()` method. The `size()` method also returned 7 instead of 8, causing the last field (`EXACT_BOUNDS`) to be silently dropped from serialized struct access.

## Example

```java
// BEFORE
public enum FieldStatistic {
  VALUE_COUNT(0, "value_count"),
  // ...
  EXACT_BOUNDS(7, "exact_bounds");

  public int offset() { return offset; }
}

// size() in BaseFieldStats
public int size() { return 7; }  // EXACT_BOUNDS at position 7 is unreachable

// StatsUtil: double-increments base
Types.StructType structType = FieldStatistic.fieldStatsFor(field.type(), fieldId + 1);

// record access used same offset as field-ID offset
record.set(VALUE_COUNT.offset(), value);  // 0 — correct for position, wrong for field ID
```

```java
// AFTER
public enum FieldStatistic {
  VALUE_COUNT(1, "value_count"),
  // ...
  EXACT_BOUNDS(8, "exact_bounds");

  public int offset() { return offset; }       // used for fieldId + offset
  public int position() { return offset - 1; } // used for record.set(pos, value)
}

// size() in BaseFieldStats
public int size() { return 8; }

// StatsUtil: no extra +1
Types.StructType structType = FieldStatistic.fieldStatsFor(field.type(), fieldId);

// record access uses position()
record.set(VALUE_COUNT.position(), value);  // 0
```
