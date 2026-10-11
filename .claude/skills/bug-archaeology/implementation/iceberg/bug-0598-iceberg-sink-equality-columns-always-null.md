# Bug 0598: IcebergSink equalityFieldColumns field always null

**Commit**: 42cac92c4847407b8b56f976b83a0602d80dce0b · **Ticket**: #14952

## Detection Rule

When a class field is declared with an inline initializer that always overrides what the constructor would assign (e.g. `private final Set<String> field = null;`), verify that the constructor parameter is actually stored and the hardcoded initializer is removed.

## Code Smell

A `private final Set<String> equalityFieldColumns = null;` declaration with a hardcoded inline initializer of `null`, accompanied by a constructor that does not assign the field — meaning the field retains `null` regardless of the builder value passed in.

## Tags

- scope: java
- category: logic-error
- code-pattern: field-init-overrides-constructor
- severity: silent-wrong-result
- subsystem: flink/sink/IcebergSink

## Root Cause

`IcebergSink` declared `equalityFieldColumns` with an inline `= null` initializer. Java final field inline initializers run after the constructor body in class instantiation, so even if the constructor had tried to assign the field, it would have been overridden to `null`. The constructor also simply did not accept or assign the value. As a result, any code using `equalityFieldColumns` for validation or error messages (e.g. checking that partition fields are covered by equality fields) always saw `null` and could not produce correct diagnostics.

## Example

```java
// BEFORE
private final Set<String> equalityFieldColumns = null;

private IcebergSink(..., FlinkMaintenanceConfig flinkMaintenanceConfig) {
    // equalityFieldColumns never assigned; remains null
}
```

```java
// AFTER
private final Set<String> equalityFieldColumns;

private IcebergSink(..., FlinkMaintenanceConfig flinkMaintenanceConfig,
                    Set<String> equalityFieldColumns) {
    ...
    this.equalityFieldColumns = equalityFieldColumns;
}
```
