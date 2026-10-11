# Bug 2579: CLI `show schema` emits deprecated `replication_factor` instead of `placement_strategy`

**Commit**: 8289d68403 · **Ticket**: CASSANDRA-3129

## Detection Rule
When `showKeyspace` writes a `replication_factor` attribute that is only set on certain keyspace definitions, and omits the required `placement_strategy` attribute for all keyspaces, the generated schema DDL is syntactically incorrect and cannot be replayed.

## Code Smell
A `writeAttr` for `placement_strategy` is placed after an `isSetReplication_factor()` guard that swallows it on keyspaces without that optional field; the keyspace DDL output omits `placement_strategy` entirely for most keyspaces.

## Tags
- scope: cassandra
- category: logic-error
- code-pattern: attribute-inside-wrong-guard
- severity: silent-wrong-result
- subsystem: tools-clients

## Root Cause
`CliClient.showKeyspace()` called `writeAttr(sb, false, "replication_factor", ...)` conditionally and placed the `placement_strategy` call inside that same `if` block. For keyspaces that do not have `replication_factor` set (e.g., NetworkTopologyStrategy), the entire block was skipped, so neither `replication_factor` nor `placement_strategy` was written. The generated `CREATE KEYSPACE` statement was thus malformed. The fix removes the `replication_factor` block entirely (it is obsolete) and unconditionally emits `placement_strategy`.

## Example
```java
// BEFORE
if (ksDef.isSetReplication_factor())
    writeAttr(sb, false, "replication_factor", ...);
writeAttr(sb, true, "placement_strategy", ...); // was inside the if block
```
```java
// AFTER
writeAttr(sb, true, "placement_strategy",
    normaliseType(ksDef.strategy_class, "org.apache.cassandra.locator"));
// replication_factor removed
```
