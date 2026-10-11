# Bug 0480: Flink Dynamic Sink case-insensitive mode still does case-sensitive field lookups

**Commit**: 8e255cd9114c4b35fb6b2d6a9e2ec0c2137b2f61 · **Ticket**: #15089

## Detection Rule

When a schema-comparison or schema-evolution visitor accepts a `caseSensitive` flag, verify that every field lookup inside the visitor (including nested struct traversal) uses the flag and calls `struct.caseInsensitiveField()` instead of `struct.field()` when the flag is false.

## Code Smell

`CompareSchemasVisitor` and `EvolveSchemaVisitor` accept a `caseSensitive` parameter at the top-level `visit()` call, but the inner `struct()` method still calls `struct.field(name)` (case-sensitive) unconditionally when iterating fields, ignoring the flag.

## Tags

- scope: project-specific
- category: wrong-filter-result
- code-pattern: else-if-masks-second
- severity: silent-wrong-result
- subsystem: flink/sink/dynamic

## Root Cause

`CompareSchemasVisitor.PartnerIdByNameAccessors` did pass the `caseSensitive` flag to top-level field resolution, but the `struct()` visitor method used hardcoded `struct.field(tableField.name())` and `struct.fields().get(i).name().equals(...)` comparisons, making case-insensitive mode behave identically to case-sensitive mode for nested struct traversal. `EvolveSchemaVisitor` had the same flaw in its struct iteration, causing unnecessary schema evolution updates or missed column matches.

## Example

```java
// BEFORE
for (Types.NestedField tableField : tableSchemaType.asStructType().fields()) {
    if (struct.field(tableField.name()) == null   // always case-sensitive
            && (tableField.isRequired() || dropUnusedColumns)) {
        return Result.INCOMPATIBLE;
    }
}
```

```java
// AFTER
for (Types.NestedField tableField : tableSchemaType.asStructType().fields()) {
    if (getFieldFromStruct(tableField.name(), struct, caseSensitive) == null
            && (tableField.isRequired() || dropUnusedColumns)) {
        return Result.INCOMPATIBLE;
    }
}

static Types.NestedField getFieldFromStruct(
        String fieldName, Types.StructType struct, boolean caseSensitive) {
    return caseSensitive ? struct.field(fieldName) : struct.caseInsensitiveField(fieldName);
}
```
