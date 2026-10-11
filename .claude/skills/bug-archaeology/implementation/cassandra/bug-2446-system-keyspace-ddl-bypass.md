# Bug 2446: CQL and Thrift allow CREATE/DROP/UPDATE on system keyspace when using another keyspace

**Commit**: b318404e53 · **Ticket**: CASSANDRA-3759 (introduced by)

## Detection Rule

When a security guard for a protected resource is tied to session state (e.g., "current keyspace"), verify that DDL operations that accept an explicit target name also validate that name independently of session state.

## Code Smell

The system keyspace guard is implemented as `if (keyspace != null && keyspace.equalsIgnoreCase(SYSTEM_TABLE) && perm == WRITE)` inside `hasKeyspaceSchemaAccess()`; this only fires when the client has set the session keyspace to "system", not when an explicit keyspace name is passed to CREATE/DROP/UPDATE.

## Tags

- scope: cassandra
- category: wrong-filter-result
- code-pattern: session-state-guard-bypassed
- severity: silent-wrong-result
- subsystem: auth-security

## Root Cause

The protection against modifying the system keyspace was implemented inside `ClientState.hasKeyspaceSchemaAccess()` by checking the session's current keyspace field. If the client's session was set to any other keyspace (or no keyspace), it could call `system_add_keyspace("system", ...)`, `system_drop_keyspace("system")`, or `system_update_keyspace("system", ...)` and bypass the guard entirely. The fix adds explicit calls to `ThriftValidation.validateKeyspaceNotSystem(name)` at each DDL entry point, before the session-level permission check.

## Example

```java
// BEFORE – guard only checks session keyspace, not the explicit argument
public void hasKeyspaceSchemaAccess(Permission perm) throws InvalidRequestException {
    if (keyspace != null && keyspace.equalsIgnoreCase(SYSTEM_TABLE) && perm == WRITE)
        throw new InvalidRequestException("system keyspace is not user-modifiable");
}
// add_keyspace("system", ...) with session on "mykeyspace" would slip through

// AFTER – explicit guard at entry points
public void add_keyspace(KsDef ks_def) throws ... {
    ThriftValidation.validateKeyspaceNotSystem(ks_def.name); // new
    state().hasKeyspaceSchemaAccess(Permission.WRITE);
    ...
}
```
