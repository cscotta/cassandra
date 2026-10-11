# Bug 2839: CFMetaData stores column_metadata as unmodifiable and NPE on null validator

**Commit**: 2d51d59a93 · **Ticket**: CASSANDRA-1768

## Detection Rule
When a constructor wraps a parameter map in `Collections.unmodifiableMap`, any subsequent method that tries to add or remove entries will throw `UnsupportedOperationException`; store a mutable copy instead if the map is mutated after construction.

## Code Smell
`this.column_metadata = Collections.unmodifiableMap(column_metadata)` prevents `apply()` from adding new column definitions or updating existing ones, causing `UnsupportedOperationException` when adding or removing secondary indexes. Also, `cfm.defaultValidator.getClass().getName()` throws NPE when `defaultValidator` is null.

## Tags
- scope: cassandra
- category: missing-null-check
- code-pattern: unmodifiable-map-mutated
- severity: crash
- subsystem: schema-metadata

## Root Cause
Two independent bugs: (1) `CFMetaData` stored its `column_metadata` as an unmodifiable map, but schema migration code calls `apply()` which iterates and updates entries — this requires a mutable `HashMap`. (2) The Avro serialization helper accessed `defaultValidator.getClass().getName()` without a null guard, crashing for CFs with no default validator configured.

## Example
```java
// BEFORE
this.column_metadata = Collections.unmodifiableMap(column_metadata);
// ...
def.default_validation_class = cfm.defaultValidator.getClass().getName();

// AFTER
this.column_metadata = new HashMap<>(column_metadata);
// ...
def.default_validation_class = cfm.defaultValidator == null
    ? null : cfm.defaultValidator.getClass().getName();
```
