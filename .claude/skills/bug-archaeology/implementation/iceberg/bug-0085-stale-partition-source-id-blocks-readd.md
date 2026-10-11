# Bug 0085: Identity partition name check blocks re-adding a schema column after source field is dropped

**Commit**: 5e3c28438570f0d526616e44d1d41f57aa49f585 · **Ticket**: #15967

## Detection Rule

When code validates a new entity name against an existing registry entry, verify that the validation is skipped or relaxed when the registry entry references a resource that no longer exists in the current state.

## Code Smell

A `Preconditions.checkArgument` that compares a new field's name against an existing field sourced from a column ID, without first checking whether that source column ID is still present in the schema. The validation fires even when the referenced source column has been dropped.

## Tags

- scope: project-specific
- category: wrong-filter-result
- code-pattern: stale-reference-blocks-valid-op
- severity: crash
- subsystem: api/partition-spec-validation

## Root Cause

`PartitionSpec.Builder` validated identity partition names by checking whether a schema field with the same name was sourced from a different column ID, but it did not check whether the original source column still exists in the schema. When a column was dropped and a new column with the same name was added, the historical partition spec's `sourceColumnId` referred to a field that no longer existed, yet the validation compared the new column's ID against the stale ID and rejected the spec as a conflict.

## Example

```java
// BEFORE
if (sourceColumnId != null) {
    Preconditions.checkArgument(
        schemaField == null || schemaField.fieldId() == sourceColumnId,
        "Cannot create identity partition sourced from different field in schema: %s",
        name);
}
```

```java
// AFTER
if (sourceColumnId != null) {
    boolean sourceFieldExists = schema.findField(sourceColumnId) != null;
    if (sourceFieldExists) {
        Preconditions.checkArgument(
            schemaField == null || schemaField.fieldId() == sourceColumnId,
            "Cannot create identity partition sourced from different field in schema: %s",
            name);
    }
}
```
