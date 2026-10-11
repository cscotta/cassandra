# Bug 2879: Bootstrap advertises NORMAL gossip state instead of BOOTSTRAPPING

**Commit**: 9e515d618b · **Ticket**: CASSANDRA-1534

## Detection Rule
When a node transitions through multiple lifecycle states, verify that the gossip `ApplicationState.STATUS` value matches the actual current state at each transition point — specifically that bootstrapping nodes never advertise themselves as normal.

## Code Smell
Inside `StorageService.bootstrap`, the first gossip state update calls `valueFactory.normal(token)` immediately after `isBootstrapMode = true`, broadcasting the node as fully joined before data streaming has begun. Other nodes use this state to decide whether to route requests to the new node.

## Tags
- scope: cassandra
- category: wrong-constant
- code-pattern: wrong-gossip-state-advertised
- severity: silent-wrong-result
- subsystem: gossip

## Root Cause
A copy-paste error used `valueFactory.normal(token)` in the bootstrap path. Peers receiving a NORMAL state immediately start routing reads and writes to the new node, which does not yet own its data. The fix changes the call to `valueFactory.bootstrapping(token)` so peers correctly treat the node as joining until `finishBootstrapping` is called.

## Example
```java
// BEFORE — advertises NORMAL state during bootstrap
isBootstrapMode = true;
Gossiper.instance.addLocalApplicationState(
    ApplicationState.STATUS, valueFactory.normal(token));
```
```java
// AFTER
isBootstrapMode = true;
Gossiper.instance.addLocalApplicationState(
    ApplicationState.STATUS, valueFactory.bootstrapping(token));
```
