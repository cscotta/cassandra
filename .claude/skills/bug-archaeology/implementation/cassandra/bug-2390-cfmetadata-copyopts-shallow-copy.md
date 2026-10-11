# Bug 2390: CFMetaData.copyOpts shares mutable ColumnDefinition objects between metadata instances

**Commit**: 11bdcd6d7f · **Ticket**: CASSANDRA-4037

## Detection Rule

When a "copy" or "clone" method assigns a mutable collection directly from the source instead of deep-copying it, mutations to one copy silently corrupt the other.

## Code Smell

`copyOpts` passes `oldCFMD.column_metadata` (a `Map<ByteBuffer, ColumnDefinition>`) directly to `.columnMetadata()` without cloning the map or its `ColumnDefinition` values — both the original and the copy share the same mutable objects.

## Tags

- scope: cassandra
- category: state-not-cleaned
- code-pattern: shallow-copy-mutable-metadata
- severity: silent-wrong-result
- subsystem: schema-metadata

## Root Cause

`CFMetaData.copyOpts` was intended to create an independent copy of a `CFMetaData` object. It copied primitive and immutable fields correctly, but assigned `oldCFMD.column_metadata` by reference and also omitted `keyAlias`, `columnAliases`, and `valueAlias`. Any modification to column definitions on the copy (e.g. during a schema migration) would silently mutate the original's map entries. The fix deep-copies each `ColumnDefinition` via a new `clone()` method and also copies the alias fields.

## Example

```java
// BEFORE
return newCFMD.comment(oldCFMD.comment)
              ...
              .columnMetadata(oldCFMD.column_metadata)  // shared reference — mutations affect original
              ...
```

```java
// AFTER
Map<ByteBuffer, ColumnDefinition> clonedColumns = new HashMap<>();
for (ColumnDefinition cd : oldCFMD.column_metadata.values()) {
    ColumnDefinition cloned = cd.clone();
    clonedColumns.put(cloned.name, cloned);
}
return newCFMD.comment(oldCFMD.comment)
              ...
              .keyAlias(oldCFMD.keyAlias)
              .columnAliases(new ArrayList<>(oldCFMD.columnAliases))
              .valueAlias(oldCFMD.valueAlias)
              .columnMetadata(clonedColumns)  // independent deep copy
              ...
```
