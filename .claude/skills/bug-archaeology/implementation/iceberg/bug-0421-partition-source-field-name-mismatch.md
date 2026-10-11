# Bug 0421: Partition field validation checked spec name instead of source field name

**Commit**: 84fa33f6e1dde6c32307066cd05d6afd061f5a4f · **Ticket**: #15190

## Detection Rule

When code checks whether a partition field is included in a set of column names, verify that the partition spec field name (e.g. "id_bucket_4") is not compared directly against schema column names — the source field must be resolved via `schema.findField(partitionField.sourceId())` first.

## Code Smell

A `Preconditions.checkState` or similar guard calling `partitionField.name()` and comparing it against a set of schema-level column names, without first resolving the source field ID to a schema field.

## Tags

- scope: project-specific
- category: logic-error
- code-pattern: name-lookup-wrong-layer
- severity: silent-wrong-result
- subsystem: flink/sink/dynamic

## Root Cause

A `PartitionField` has two names: the partition spec field name (which may be a transformed name like "id_bucket_4") and the name of the source column in the schema. The validation was calling `partitionField.name()`, which returns the partition spec name, and checking it against `equalityFields` (a set of schema column names). This meant the check always failed for transform-based partitions (e.g. bucket), incorrectly rejecting valid configurations where the source column was present in equality fields.

## Example

```java
// BEFORE
for (PartitionField partitionField : spec.fields()) {
    Preconditions.checkState(
        equalityFields.contains(partitionField.name()),
        ...);
}
```

```java
// AFTER
for (PartitionField partitionField : spec.fields()) {
    Types.NestedField sourceField = schema.findField(partitionField.sourceId());
    Preconditions.checkState(
        sourceField != null && equalityFields.contains(sourceField.name()),
        ...);
}
```
