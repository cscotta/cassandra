# Bug 0418: Partition field equality check uses transform name instead of source column name

**Commit**: 995101f17999d09602af4bc43fe8aad351c10251 · **Ticket**: #15190

## Detection Rule

When checking whether a partition field is present in a set of column names, verify that the lookup uses the partition field's source column name (via `schema.findField(partitionField.sourceId()).name()`) rather than `partitionField.name()`, which is the name of the transform application and may differ from the source column.

## Code Smell

A `PartitionField` object is used in a `contains()` check against a `Set<String>` of user-provided column names, with `partitionField.name()` supplying the lookup key. A reviewer can spot this by searching for `partitionField.name()` inside equality-field validation logic.

## Tags

- scope: project-specific
- category: wrong-filter-result
- code-pattern: partition-name-vs-source-name
- severity: crash
- subsystem: flink/dynamic-sink

## Root Cause

`PartitionField.name()` returns the name of the partition transform field (e.g., `id_bucket_4` for a bucket transform on `id`), whereas equality fields are expressed as source column names (e.g., `id`). The validation in `HashKeyGenerator` compared `partitionField.name()` against the user-supplied equality field names, so a table partitioned by `bucket(id, 4)` with equality field `id` always failed the check and threw `IllegalStateException`, making it impossible to use hash distribution with bucketed partition specs.

## Example

```java
// BEFORE — compares transform name (e.g. "id_bucket_4") to column name (e.g. "id")
for (PartitionField partitionField : spec.fields()) {
    Preconditions.checkState(
        equalityFields.contains(partitionField.name()),
        "partition field '%s' should be included in equality fields",
        partitionField.name(), equalityFields);
}
```

```java
// AFTER — resolves source column name via schema before comparing
for (PartitionField partitionField : spec.fields()) {
    Types.NestedField sourceField = schema.findField(partitionField.sourceId());
    Preconditions.checkState(
        sourceField != null && equalityFields.contains(sourceField.name()),
        "partition field '%s' should be included in equality fields",
        partitionField.name(), equalityFields);
}
```
