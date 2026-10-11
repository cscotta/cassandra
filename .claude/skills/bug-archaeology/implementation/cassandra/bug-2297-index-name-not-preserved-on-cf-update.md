# Bug 2297: CF Update Rejects Unchanged Index Name Due to Missing Backfill of Existing Names

**Commit**: 202f3940dc · **Ticket**: CASSANDRA-4439

## Detection Rule

When enforcing immutability of a field during an update operation, first copy the existing value into the incoming object so that a no-op update (field unchanged) does not trigger a spurious "field was changed" error.

## Code Smell

A validation that checks `!oldValue.equals(newValue)` fires even when the user did not specify a new value, because the incoming object carries a default/null for that field rather than the existing persisted value.

## Tags

- scope: cassandra
- category: logic-error
- code-pattern: missing-existing-value-backfill
- severity: crash
- subsystem: schema-metadata

## Root Cause

When updating a column family, the incoming `CFMetaData` object had `null` for index names of columns that the user did not mention. The validation code compared this `null` against the existing non-null index name and threw `ConfigurationException("Can't modify index name")` even though the user made no change. The fix loads the existing index name into the incoming `ColumnDefinition` (`newDef.setIndexName(oldIndexName)`) before validation, so that columns not touched by the update retain their old names.

## Example

```java
// BEFORE — newDef.getIndexName() is null for untouched columns; validation fires spuriously
if (newDef.getIndexName() != null && !oldIndexName.equals(newDef.getIndexName()))
    throw new ConfigurationException("Can't modify index name: ...");
// newDef still has null index name — default name generation proceeds incorrectly
```

```java
// AFTER — backfill existing name before validation
if (newDef.getIndexName() != null && !oldIndexName.equals(newDef.getIndexName()))
    throw new ConfigurationException("Can't modify index name: ...");
newDef.setIndexName(oldIndexName);  // preserve existing name for unmodified columns
```
