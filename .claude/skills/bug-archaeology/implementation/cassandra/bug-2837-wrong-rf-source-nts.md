# Bug 2837: Consistency level calculations use global RF instead of per-strategy RF

**Commit**: 2f5f0c2c12 · **Ticket**: CASSANDRA-1804

## Detection Rule
When computing quorum or replication-factor-dependent values, always obtain the RF from the table's own `ReplicationStrategy`, not from `DatabaseDescriptor.getReplicationFactor(table)`, which returns a global default that is wrong for `NetworkTopologyStrategy`.

## Code Smell
`DatabaseDescriptor.getReplicationFactor(table)` returns the single global `replicationFactor` field set in `KSMetaData`, which is only meaningful for `SimpleStrategy`. `NetworkTopologyStrategy` tracks RF per-DC and overrides `getReplicationFactor()` on the strategy object; calling the descriptor method returns 0 or a wrong value.

## Tags
- scope: cassandra
- category: logic-error
- code-pattern: wrong-rf-source
- severity: silent-wrong-result
- subsystem: network-messaging

## Root Cause
`QuorumResponseHandler.blockForValue`, `WriteResponseHandler.determineBlockFor`, and `StorageService.waitForSchemaAgreement` all called `DatabaseDescriptor.getReplicationFactor(table)`. For `NetworkTopologyStrategy` this value is incorrect. The fix calls `Table.open(table).getReplicationStrategy().getReplicationFactor()` everywhere, which dispatches to the correct per-strategy implementation.

## Example
```java
// BEFORE
case QUORUM:
    return (DatabaseDescriptor.getReplicationFactor(table) / 2) + 1;
case ALL:
    return DatabaseDescriptor.getReplicationFactor(table);

// AFTER
case QUORUM:
    return (Table.open(table).getReplicationStrategy().getReplicationFactor() / 2) + 1;
case ALL:
    return Table.open(table).getReplicationStrategy().getReplicationFactor();
```
