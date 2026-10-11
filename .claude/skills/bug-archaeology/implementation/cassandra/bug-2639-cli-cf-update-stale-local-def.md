# Bug 2639: CLI column family update uses local stale CfDef, propagating inferred type fields to server

**Commit**: 83f9f455d7 · **Ticket**: CASSANDRA-2809

## Detection Rule
When applying a schema update through a remote API, always fetch the current definition from the server rather than using a locally-cached copy, which may contain stale or inferred fields that should not be sent back.

## Code Smell
`executeUpdateColumnFamily()` calls `getCfDef(cfName)` which returns a locally-built `CfDef` object that may contain type fields inferred from other schema state; these inferred values should not be included in the update payload sent to `system_update_column_family`.

## Tags
- scope: cassandra
- category: wrong-filter-result
- code-pattern: stale-local-schema-used
- severity: silent-wrong-result
- subsystem: tools-clients

## Root Cause
The CLI's `executeUpdateColumnFamily()` obtained the column family definition from an in-memory local map (`getCfDef(cfName)`), which could contain type fields that were inferred or derived locally and should not be sent as part of an update. The fix fetches the current `CfDef` directly from the server via `thriftClient.describe_keyspace(this.keySpace)`, ensuring only the server's authoritative definition is used as the base for the update, preventing spurious type fields from corrupting the schema.

## Example
```java
// BEFORE
CfDef cfDef = getCfDef(cfName);  // local cached copy, may have inferred types

// AFTER
CfDef cfDef = getCfDef(thriftClient.describe_keyspace(this.keySpace), cfName);
// fetches current definition from server, no inferred types
```
