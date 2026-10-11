# Bug 2447: NPE in ClientState.hasKeyspaceSchemaAccess when session keyspace is null

**Commit**: 7c37454779 · **Ticket**: CASSANDRA-3759 (introduced by)

## Detection Rule

When adding a null guard to a string comparison (`s.equalsIgnoreCase(...)`) after the fact, check that the guard was placed on the correct side to avoid the NPE; use `s != null &&` before the method call.

## Code Smell

`keyspace.equalsIgnoreCase(Table.SYSTEM_TABLE)` is called without a null check on `keyspace`; calling any DDL operation without first selecting a keyspace triggers an NPE.

## Tags

- scope: cassandra
- category: missing-null-check
- code-pattern: missing-null-check-before-method-call
- severity: crash
- subsystem: auth-security

## Root Cause

CASSANDRA-3759 added a system keyspace write-protection check: `if (keyspace.equalsIgnoreCase(SYSTEM_TABLE) && perm == WRITE)`. When the client had not yet selected a keyspace, `keyspace` was null, so calling any schema-modifying operation without first using `SET KEYSPACE` caused an NPE. The fix adds a null check: `if (keyspace != null && keyspace.equalsIgnoreCase(SYSTEM_TABLE) && ...)`.

## Example

```java
// BEFORE – NPE when keyspace is null
if (keyspace.equalsIgnoreCase(Table.SYSTEM_TABLE) && perm == Permission.WRITE)
    throw new InvalidRequestException("system keyspace is not user-modifiable");

// AFTER
if (keyspace != null && keyspace.equalsIgnoreCase(Table.SYSTEM_TABLE) && perm == Permission.WRITE)
    throw new InvalidRequestException("system keyspace is not user-modifiable");
```
