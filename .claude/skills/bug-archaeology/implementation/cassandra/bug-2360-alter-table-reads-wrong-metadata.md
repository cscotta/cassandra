# Bug 2360: CQL3 ALTER TABLE reads column definition from wrong metadata object

**Commit**: 17e3cc42e0 · **Ticket**: CASSANDRA-4170

## Detection Rule

When an ALTER TABLE implementation retrieves a `ColumnDefinition` to modify, verify the lookup is performed on the working copy (`cfm`) being built, not on a stale or different object (e.g., the original `meta` retrieved from the schema registry).

## Code Smell

`ColumnDefinition column = meta.getColumnDefinition(columnName.key)` inside an `ALTER COLUMN` branch uses `meta` (the schema registry object) rather than `cfm` (the modifiable copy), so any changes made to `column` — and re-added via `cfm.addColumnDefinition(column)` — may reference a stale definition or introduce a stale object from a different version.

## Tags

- scope: cassandra
- category: logic-error
- code-pattern: wrong-source-object-lookup
- severity: silent-wrong-result
- subsystem: schema-metadata

## Root Cause

`AlterTableStatement.announceMigration()` obtained a `CFMetaData` working copy in `cfm` for modification, but the `COLUMN_METADATA` case fetched the `ColumnDefinition` to update from `meta` (the original registry object) instead of `cfm`. When `cfm.addColumnDefinition(column)` was subsequently called, it added the column from the wrong source, so the validator update was not applied to the correct working copy. The single-character fix changes `meta` to `cfm`.

## Example

```java
// BEFORE
case COLUMN_METADATA:
    ColumnDefinition column = meta.getColumnDefinition(columnName.key); // wrong object
    column.setValidator(CFPropDefs.parseType(validator));
    cfm.addColumnDefinition(column);
```

```java
// AFTER
case COLUMN_METADATA:
    ColumnDefinition column = cfm.getColumnDefinition(columnName.key); // correct object
    column.setValidator(CFPropDefs.parseType(validator));
    cfm.addColumnDefinition(column);
```
