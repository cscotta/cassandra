# Bug 2706: Column validator not updated when altering column metadata

**Commit**: 6dc75ce2f3 · **Ticket**: CASSANDRA-2512

## Detection Rule
When updating an existing record that has mutable fields, check that every mutable field (not just index metadata) is actually written back.

## Code Smell
An "update existing" loop checks membership but only updates index fields, silently leaving the validator unchanged; the validator field is also `public final` so it cannot be mutated even if referenced directly.

## Tags
- scope: cassandra
- category: state-not-cleaned
- code-pattern: partial-update-missing-field
- severity: silent-wrong-result
- subsystem: schema-metadata

## Root Cause
`CFMetaData.apply()` iterated `cf_def.column_metadata` and updated `index_type` and `index_name` on existing `ColumnDefinition` objects, but never called any setter for the `validator` field. Additionally, `validator` was declared `public final`, making it physically immutable. The fix changes the field to `private` with a `setValidator()` accessor and calls it during the update loop.

## Example
```java
// BEFORE (only index fields updated, validator never changed)
column_metadata.get(def.name).setIndexType(...);
column_metadata.get(def.name).setIndexName(...);
```
```java
// AFTER (validator updated along with index fields)
oldDef.setValidator(DatabaseDescriptor.getComparator(def.validation_class));
oldDef.setIndexType(...);
oldDef.setIndexName(...);
```
