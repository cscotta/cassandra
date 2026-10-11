# Bug 2581: LocalStrategy allowed as user keyspace replication strategy

**Commit**: fd53a7efb4 · **Ticket**: CASSANDRA-3139

## Detection Rule
When `ThriftValidation.validateReplicationStrategy` does not reject `LocalStrategy` before attempting to instantiate it, users can create keyspaces with a strategy reserved for internal system tables, corrupting cluster metadata behaviour.

## Code Smell
`AbstractReplicationStrategy.getClass(ks_def.strategy_class)` succeeds for `LocalStrategy` without a subsequent guard that rejects it, so `createReplicationStrategy` is called with a class that should never be used for user keyspaces.

## Tags
- scope: cassandra
- category: missing-null-check
- code-pattern: missing-class-guard-validation
- severity: data-loss
- subsystem: schema-metadata

## Root Cause
Cassandra uses `LocalStrategy` internally for system tables. There was no validation guard preventing users from specifying it explicitly when creating a keyspace via Thrift or CQL. A user-created keyspace with `LocalStrategy` would bypass replication entirely or produce inconsistent results. The fix adds an explicit check immediately after resolving the strategy class.

## Example
```java
// BEFORE
Class<? extends AbstractReplicationStrategy> cls =
    AbstractReplicationStrategy.getClass(ks_def.strategy_class);
// no guard — LocalStrategy accepted
AbstractReplicationStrategy.createReplicationStrategy(..., cls, ...);
```
```java
// AFTER
Class<? extends AbstractReplicationStrategy> cls =
    AbstractReplicationStrategy.getClass(ks_def.strategy_class);
if (cls.equals(LocalStrategy.class))
    throw new ConfigurationException(
        "LocalStrategy is reserved for internal use.");
AbstractReplicationStrategy.createReplicationStrategy(..., cls, ...);
```
