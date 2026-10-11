# Bug 0406: Avro ROW_ID metadata column not incremented when data file lacks row-lineage fields

**Commit**: 232af57377bcd796c7d69d57854b39f996dfd9d0 · **Ticket**: #15187

## Detection Rule

When a metadata column is injected as a constant during schema evolution reads, verify that any column whose semantics require per-row mutation (e.g. a monotonically-increasing row ID) is routed through its dedicated reader rather than the generic constant reader.

## Code Smell

A `constant != null` guard returns `ValueReaders.constant(constant)` for all field IDs without first checking whether the field ID is `ROW_ID`, which requires `ValueReaders.rowIds(base, null)` to auto-increment across rows.

## Tags

- scope: project-specific
- category: wrong-filter-result
- code-pattern: constant-reader-wrong-for-stateful-column
- severity: silent-wrong-result
- subsystem: core/avro

## Root Cause

`ValueReaders` dispatches reader construction for missing file columns using `ValueReaders.constant(constant)`, which returns the same value for every row. The `ROW_ID` metadata column must instead use `ValueReaders.rowIds(baseRowId, null)` so that each row in the file gets a unique, incrementing ID. Because the special case was absent, every row in a data file that predates row-lineage received the same `ROW_ID` value.

## Example

```java
// BEFORE
if (constant != null) {
    return ValueReaders.constant(constant);  // same value for every row
}
```

```java
// AFTER
if (constant != null) {
    if (fieldId == MetadataColumns.ROW_ID.fieldId()) {
        return ValueReaders.rowIds((Long) constant, null);  // increments per row
    }
    return ValueReaders.constant(constant);
}
```
