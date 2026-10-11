# Bug 2552: Bootstrap process skipped due to inverted condition

**Commit**: 5e4d3055e9 · **Ticket**: CASSANDRA-3285

## Detection Rule
When a bootstrap guard condition uses `isEmpty()` where it should use `!isEmpty()`, nodes that already have schema but have not yet bootstrapped will skip bootstrap and join the ring without transferring data.

## Code Smell
A boolean expression in a startup path reads `|| Schema.instance.getNonSystemTables().isEmpty()` (proceed if no tables exist) where the intent is to proceed when tables **do** exist (non-empty schema means we need bootstrap data).

## Tags
- scope: cassandra
- category: logic-error
- code-pattern: inverted-boolean-guard
- severity: data-loss
- subsystem: bootstrap-startup

## Root Cause
The condition `!Schema.instance.getNonSystemTables().isEmpty()` was written as `Schema.instance.getNonSystemTables().isEmpty()`. This inverted the semantics: nodes with a non-empty schema (which need bootstrapping) skipped the bootstrap process entirely and joined the ring without copying data. Additionally, the `setBootstrapped(true)` / `setToken(token)` calls were inside the `if` block instead of unconditionally following it, so nodes that did not bootstrap never became fully operational.

## Example
```java
// BEFORE
if (isAutoBootstrap
    && !(isBootstrapped
         || isSeeded
         || Schema.instance.getNonSystemTables().isEmpty())) {
    // bootstrap
    ...
    SystemTable.setBootstrapped(true);  // inside if
    setToken(token);
}
```
```java
// AFTER
if (isAutoBootstrap
    && !(isBootstrapped
         || isSeeded
         || !Schema.instance.getNonSystemTables().isEmpty())) {
    // bootstrap
}
// unconditionally join ring
SystemTable.setBootstrapped(true);
setToken(token);
```
