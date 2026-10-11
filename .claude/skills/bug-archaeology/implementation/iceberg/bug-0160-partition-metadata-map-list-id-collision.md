# Bug 0160: NPE in PruneColumns when MAP/LIST columns cause _partition child ID collision

**Commit**: 63ecc7c3871ec1a1846c2d335c2055ea1865a81f · **Ticket**: #15726

## Detection Rule

When building a set of "used field IDs" across a schema for the purpose of avoiding ID collisions with synthetic metadata columns, verify that the ID-collection utility includes IDs for all structural node types (MAP key/value IDs, LIST element IDs), not only projected column IDs.

## Code Smell

A call to `TypeUtil.getProjectedIds(schema)` used in a context that is not column projection but ID-space reservation — `getProjectedIds` omits MAP/LIST structural IDs, so callers relying on it for collision avoidance will produce incorrect results when the schema contains those types.

## Tags

- scope: project-specific
- category: missing-null-check
- code-pattern: wrong-id-collection-utility
- severity: crash
- subsystem: spark/source/BaseSparkScanBuilder

## Root Cause

`BaseSparkScanBuilder.allUsedFieldIds()` called `TypeUtil.getProjectedIds()` which is designed for column projection and intentionally skips MAP key/value and LIST element field IDs. When the `_partition` metadata struct was assigned field IDs starting after the highest projected ID, those IDs could collide with the omitted MAP/LIST structural IDs. During merge-on-read scans where `_partition` is added to the projection, `PruneColumns.isStruct()` would then encounter a type mismatch and throw a NPE. The fix uses `TypeUtil.indexById()` which indexes all field IDs recursively, correctly reserving the entire ID space.

## Example

```java
// BEFORE
private Set<Integer> allUsedFieldIds() {
    return table.schemas().values().stream()
        .flatMap(tableSchema ->
            TypeUtil.getProjectedIds(tableSchema.asStruct()).stream()) // omits MAP/LIST IDs
        .collect(Collectors.toSet());
}
```

```java
// AFTER
private Set<Integer> allUsedFieldIds() {
    return table.schemas().values().stream()
        .flatMap(tableSchema ->
            TypeUtil.indexById(tableSchema.asStruct()).keySet().stream()) // includes all IDs
        .collect(Collectors.toSet());
}
```
