# Bug 0387: Equality delete field lookup fails after column is dropped from schema

**Commit**: 00df4934a6b66c9f36025f45eae1fd2f7588f71f · **Ticket**: #15268

## Detection Rule

When equality delete files reference field IDs by ID, and field lookup for boundary conversion or projection uses only the current table schema, verify that the code also consults historic schemas so that fields present at delete-write time but since dropped are still resolvable.

## Code Smell

Code that resolves equality delete field IDs via `spec.schema().findField(id)` or `tableSchema.asStruct().field(fieldId)` where `spec.schema()` is the partition spec's schema (a single snapshot-era schema) rather than the full schema history — produces a `null` field reference when the column was dropped after the delete file was written.

## Tags

- scope: project-specific
- category: missing-null-check
- code-pattern: null-after-map-lookup
- severity: crash
- subsystem: core/delete-file-index

## Root Cause

`EqualityDeleteFile.equalityFields()` looked up field IDs against `spec.schema()` (the schema that was current when the partition spec was created). After a schema evolution that dropped the equality-delete column, `findField()` returned null, causing a NullPointerException or incorrect boundary conversion. The fix adds a multi-schema field index built from all historic schemas (preferring higher schema IDs for type promotions) so dropped columns remain resolvable.

## Example

```java
// BEFORE — spec.schema() knows nothing about dropped columns
List<Types.NestedField> fields = Lists.newArrayList();
for (int id : wrapped.equalityFieldIds()) {
    Types.NestedField field = spec.schema().findField(id);  // null after column drop
    fields.add(field);
}

// boundary conversion also used spec.schema()
Type type = spec.schema().findField(id).type();  // NPE
```

```java
// AFTER — fieldLookup indexes all historic schemas
EqualityDeleteFile(Function<Integer, Types.NestedField> fieldLookup, DeleteFile file) {
    this.fieldLookup = fieldLookup;
    ...
}

for (int id : wrapped.equalityFieldIds()) {
    Types.NestedField field = fieldLookup.apply(id);
    Preconditions.checkArgument(field != null, "Cannot find field for ID %s", id);
    fields.add(field);
}
Type type = field.type();  // resolved from multi-schema index
```
