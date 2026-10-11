# Bug 0219: Avro reader crashes when ROW_LINEAGE metadata column exists in file but is not projected

**Commit**: 49a9f9946a04121dcf7a9cb9c3f5c68837301dee · **Ticket**: #15508

## Detection Rule

When branching on a special field ID (e.g. a metadata/lineage column) inside a reader-building method, verify that a null projection position is handled as a short-circuit "skip this field" case before any special-ID checks, because special handling may assume the column is projected and dereference a null position.

## Code Smell

A chain of `if (fieldId == SPECIAL_ID)` branches inside a reader factory method where `projectionPos` can be null (field not projected), but the null check for `projectionPos` comes after the special-ID branches that unconditionally use it.

## Tags

- scope: project-specific
- category: missing-null-check
- code-pattern: null-before-special-case-check
- severity: crash
- subsystem: core/avro/ValueReaders

## Root Cause

`ValueReaders.readerForMetadataColumn` checked for `ROW_ID` and `LAST_UPDATED_SEQUENCE_NUMBER` field IDs before checking whether `projectionPos` was null. When a file physically contains those row-lineage columns but the read schema does not project them, `projectionPos` is null and the special-ID branches attempted to use it, causing a NullPointerException. The fix moves the `projectionPos == null` guard to the top of the method so unprojected fields are immediately routed to a skip-only reader.

## Example

```java
// BEFORE
if (Objects.equals(fieldId, MetadataColumns.ROW_ID.fieldId())) {
    Long firstRowId = (Long) idToConstant.get(fieldId);
    return Pair.of(projectionPos, ValueReaders.rowIds(firstRowId, fieldReader)); // NPE if projectionPos==null
} else if (Objects.equals(fieldId, MetadataColumns.LAST_UPDATED_SEQUENCE_NUMBER.fieldId())) {
    ...
}
```

```java
// AFTER
if (projectionPos == null) {
    // field is in the file but not projected; keep the reader only for skipping
    return Pair.of(null, fieldReader);
} else if (Objects.equals(fieldId, MetadataColumns.ROW_ID.fieldId())) {
    Long firstRowId = (Long) idToConstant.get(fieldId);
    return Pair.of(projectionPos, ValueReaders.rowIds(firstRowId, fieldReader));
}
```
