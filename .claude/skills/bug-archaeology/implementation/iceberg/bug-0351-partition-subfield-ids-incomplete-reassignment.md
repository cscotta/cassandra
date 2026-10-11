# Bug 0351: Partition metadata sub-field IDs not fully collected for conflict-free reassignment

**Commit**: 4c2e60d1541fb37d6ea6be31b2ea2cf733a087c3 · **Ticket**: #15297

## Detection Rule

When computing a set of IDs to reassign for conflict avoidance in a schema that contains nested/struct fields, verify that the collection traverses all nested field IDs recursively rather than only the top-level IDs of the struct.

## Code Smell

`TypeUtil.indexById(structType).keySet()` used to gather a set of "IDs to reassign" from a nested struct, where `indexById` only returns the direct children's IDs rather than all recursive sub-field IDs; the resulting set silently omits deeply nested partition sub-field IDs.

## Tags

- scope: project-specific
- category: wrong-filter-result
- code-pattern: incomplete-nested-id-collection
- severity: silent-wrong-result
- subsystem: spark/metadata-columns

## Root Cause

The old `calculateMetadataSchema` used `TypeUtil.indexById(partitionField.get().type().asStructType()).keySet()` to build `idsToReassign`, but `indexById` returns only direct-child field IDs, not a recursive traversal. For partition types with deeply-nested struct sub-fields, the inner sub-field IDs were excluded from the reassignment set, meaning those IDs were kept as-is and could collide with data field IDs. The fix replaces this with `TypeUtil.getProjectedIds(partitionType)`, which recursively collects all nested field IDs.

## Example

```java
// BEFORE
Set<Integer> idsToReassign =
    TypeUtil.indexById(partitionField.get().type().asStructType()).keySet();

Set<Integer> currentlyUsedIds =
    metaColumnFields.stream().map(Types.NestedField::fieldId).collect(Collectors.toSet());
Set<Integer> allUsedIds =
    table.schemas().values().stream()
        .map(currSchema -> TypeUtil.indexById(currSchema.asStruct()).keySet())
        .reduce(currentlyUsedIds, Sets::union);
```

```java
// AFTER
Types.StructType partitionType = partitionField.get().type().asStructType();
Set<Integer> partitionFieldIds = TypeUtil.getProjectedIds(partitionType);
GetID getId = TypeUtil.reassignConflictingIds(partitionFieldIds, allUsedFieldIds());

// allUsedFieldIds() uses getProjectedIds to recursively collect all nested IDs
private Set<Integer> allUsedFieldIds() {
    return table.schemas().values().stream()
        .flatMap(tableSchema -> TypeUtil.getProjectedIds(tableSchema.asStruct()).stream())
        .collect(Collectors.toSet());
}
```
