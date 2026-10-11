# Bug 2942: Anti-entropy repair runs on the system keyspace, corrupting internal metadata

**Commit**: 5b86c3dc00 · **Ticket**: CASSANDRA-616

## Detection Rule
When anti-entropy validation logic checks only for a null initiator but does not exclude special internal keyspaces, Merkle tree repair can run against system tables and attempt to overwrite internal metadata.

## Code Smell
`getValidator()` returns a `NoopValidator` only when `initiator == null`, missing the additional check `|| table.equals(Table.SYSTEM_TABLE)` that guards the system keyspace from being included in anti-entropy repairs.

## Tags
- scope: cassandra
- category: wrong-filter-result
- code-pattern: missing-system-keyspace-guard
- severity: data-loss
- subsystem: repair

## Root Cause
`AntiEntropyService.getValidator()` decided whether to run a real validation or a no-op based solely on whether `initiator` was null. The system table contains internal Cassandra metadata (tokens, schema, hints) that should never be included in user-triggered repair. By also returning a `NoopValidator` when `table.equals(Table.SYSTEM_TABLE)`, repair is prevented from modifying system-level data.

## Example
```java
// BEFORE
public IValidator getValidator(String table, String cf, InetAddress initiator) {
    if (initiator == null)
        return new NoopValidator();  // system table not excluded
    ...
}
```
```java
// AFTER
public IValidator getValidator(String table, String cf, InetAddress initiator) {
    if (initiator == null || table.equals(Table.SYSTEM_TABLE))
        return new NoopValidator();  // system table skipped
    ...
}
```
