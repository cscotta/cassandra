# Bug 0170: Field stats schema includes inapplicable sub-fields for all column types

**Commit**: 72b5f8e1eab614a9b946308d8a6eb9f20a90ea3e · **Ticket**: #15739

## Detection Rule

When constructing a per-field statistics struct, if the set of sub-fields is unconditionally fixed regardless of the column's type or nullability, verify that every sub-field is semantically valid for the column — including `null_value_count` for required (non-nullable) columns, `nan_value_count` for non-floating-point types, and size fields for non-variable-length types produces a schema that misrepresents what statistics can actually be collected.

## Code Smell

A `Types.StructType.of(...)` call lists all 8 statistic fields as a static block — `value_count`, `null_value_count`, `nan_value_count`, `avg_value_size`, `max_value_size`, `lower_bound`, `upper_bound`, `exact_bounds` — without any conditional guards on type or nullability.

## Tags

- scope: project-specific
- category: wrong-serialization
- code-pattern: unconditional-schema-fields
- severity: silent-wrong-result
- subsystem: core/stats

## Root Cause

`FieldStatistic.fieldStatsFor(Type type, int baseFieldId)` accepted only the raw `Type`, discarding whether the field was required or optional and ignoring type-specific semantics. It always emitted all 8 sub-fields, so required columns got a `null_value_count` sub-field that can never be populated, non-numeric columns got `nan_value_count`, and non-string/binary columns got `avg_value_size`/`max_value_size`. The signature was changed to accept a `Types.NestedField` so nullability and type ID can gate which sub-fields are included.

## Example

```java
// BEFORE
public static Types.StructType fieldStatsFor(Type type, int baseFieldId) {
    return Types.StructType.of(
        optional(baseFieldId + VALUE_COUNT.offset(), VALUE_COUNT.fieldName(), LongType.get(), ...),
        optional(baseFieldId + NULL_VALUE_COUNT.offset(), NULL_VALUE_COUNT.fieldName(), ...),
        optional(baseFieldId + NAN_VALUE_COUNT.offset(), NAN_VALUE_COUNT.fieldName(), ...),
        optional(baseFieldId + AVG_VALUE_SIZE.offset(), AVG_VALUE_SIZE.fieldName(), ...),
        optional(baseFieldId + MAX_VALUE_SIZE.offset(), MAX_VALUE_SIZE.fieldName(), ...),
        optional(baseFieldId + LOWER_BOUND.offset(), LOWER_BOUND.fieldName(), type, ...),
        optional(baseFieldId + UPPER_BOUND.offset(), UPPER_BOUND.fieldName(), type, ...),
        optional(baseFieldId + EXACT_BOUNDS.offset(), EXACT_BOUNDS.fieldName(), ...));
}
```

```java
// AFTER
public static Types.StructType fieldStatsFor(Types.NestedField field, int baseFieldId) {
    List<Types.NestedField> fields = Lists.newArrayListWithCapacity(8);
    Type type = field.type();
    fields.add(optional(baseFieldId + VALUE_COUNT.offset(), VALUE_COUNT.fieldName(), ...));
    if (field.isOptional()) {
        fields.add(optional(baseFieldId + NULL_VALUE_COUNT.offset(), ...));
    }
    if (type.typeId() == Type.TypeID.FLOAT || type.typeId() == Type.TypeID.DOUBLE) {
        fields.add(optional(baseFieldId + NAN_VALUE_COUNT.offset(), ...));
    }
    if (type.typeId() == Type.TypeID.STRING || type.typeId() == Type.TypeID.BINARY) {
        fields.add(optional(baseFieldId + AVG_VALUE_SIZE.offset(), ...));
        fields.add(optional(baseFieldId + MAX_VALUE_SIZE.offset(), ...));
    }
    fields.add(optional(baseFieldId + LOWER_BOUND.offset(), LOWER_BOUND.fieldName(), type, ...));
    fields.add(optional(baseFieldId + UPPER_BOUND.offset(), UPPER_BOUND.fieldName(), type, ...));
    fields.add(optional(baseFieldId + EXACT_BOUNDS.offset(), ...));
    return Types.StructType.of(fields);
}
```
