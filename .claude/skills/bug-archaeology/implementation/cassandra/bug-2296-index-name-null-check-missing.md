# Bug 2296: NPE When Index Column Has No Previous Index Name During CF Update

**Commit**: e220efa2a8 · **Ticket**: n/a

## Detection Rule

When iterating over column metadata to enforce immutability of an existing field, guard against the case where that field was never set on the existing column before comparing it to the proposed new value.

## Code Smell

`oldIndexName.equals(newDef.getIndexName())` is called without a null check on `oldIndexName`, assuming every indexed column in the existing schema already has a name assigned.

## Tags

- scope: cassandra
- category: missing-null-check
- code-pattern: null-field-dereference-on-existing-record
- severity: crash
- subsystem: schema-metadata

## Root Cause

`addDefaultIndexNames` iterates over column definitions to check that an existing index name is not being changed. The code fetched `oldIndexName` from the existing column metadata but did not guard against `oldIndexName` being null (i.e., the column was indexed but had no name set yet). Calling `oldIndexName.equals(...)` with a null `oldIndexName` caused an NPE. The fix adds `if (oldIndexName == null) continue` before the comparison.

## Example

```java
// BEFORE
String oldIndexName = cfm.column_metadata.get(entry.getKey()).getIndexName();
// oldIndexName may be null if no name was previously set
if (newDef.getIndexName() != null && !oldIndexName.equals(newDef.getIndexName()))
    throw new ConfigurationException("Can't modify index name...");
```

```java
// AFTER
String oldIndexName = cfm.column_metadata.get(entry.getKey()).getIndexName();
if (oldIndexName == null)
    continue;
if (newDef.getIndexName() != null && !oldIndexName.equals(newDef.getIndexName()))
    throw new ConfigurationException("Can't modify index name...");
```
