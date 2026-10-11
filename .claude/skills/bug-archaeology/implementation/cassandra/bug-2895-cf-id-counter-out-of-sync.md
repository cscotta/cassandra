# Bug 2895: Column family ID counter gets out of sync when CFs are added remotely

**Commit**: 29c38c8035 · **Ticket**: CASSANDRA-1403

## Detection Rule
When a schema migration adds a column family definition that may have arrived from a remote node (with a pre-assigned ID), call `CFMetaData.fixMaxId()` after updating the local descriptor to ensure the local ID generator is advanced past the imported ID.

## Code Smell
`AddColumnFamily.applyModels` calls `DatabaseDescriptor.setTableDefinition(ksm, newVersion)` and then `Table.open(...).initCf(...)` without calling `CFMetaData.fixMaxId()`. A schema change received from another node carries a CF ID assigned there; the local generator is unaware and may reuse that ID for the next locally-created CF.

## Tags
- scope: cassandra
- category: state-not-cleaned
- code-pattern: id-counter-not-advanced-after-import
- severity: silent-wrong-result
- subsystem: schema-metadata

## Root Cause
`CFMetaData.fixMaxId()` scans all known CF IDs and advances the static counter to the current maximum. Without this call after importing a remote schema definition, the local counter stays below the imported ID and the next `AddColumnFamily` locally would assign a duplicate ID, causing CF-confusion bugs at read/write time.

## Example
```java
// BEFORE — fixMaxId not called, counter may be below imported CF ID
DatabaseDescriptor.setTableDefinition(ksm, newVersion);
if (!clientMode)
    Table.open(ksm.name).initCf(cfm.cfId, cfm.cfName);
```
```java
// AFTER
DatabaseDescriptor.setTableDefinition(ksm, newVersion);
CFMetaData.fixMaxId();  // advance counter past any imported ID
if (!clientMode)
    Table.open(ksm.name).initCf(cfm.cfId, cfm.cfName);
```
