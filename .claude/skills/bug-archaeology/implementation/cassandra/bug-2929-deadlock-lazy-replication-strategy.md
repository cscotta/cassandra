# Bug 2929: Deadlock in replication strategy lookup during initialization

**Commit**: 75daa13317 · **Ticket**: CASSANDRA-778

## Detection Rule
When a `synchronized` method lazily creates an object that itself triggers gossip or token-metadata callbacks that also need the same lock, a deadlock occurs; pre-creating all instances during single-threaded startup avoids the cycle.

## Code Smell
`synchronized getReplicationStrategy()` that lazily instantiates strategies on first call; the constructor of `AbstractReplicationStrategy` registers with `TokenMetadata`, which can call back into `StorageService` under a different lock.

## Tags
- scope: cassandra
- category: deadlock
- code-pattern: lazy-init-under-lock-with-callback
- severity: hang
- subsystem: bootstrap-startup

## Root Cause
`StorageService.getReplicationStrategy()` was `synchronized` and lazily created `AbstractReplicationStrategy` objects on first access. The strategy constructor registers with `TokenMetadata`, which may notify gossip subscribers that call back into `StorageService` methods requiring the same monitor, causing a deadlock. The fix eagerly creates all replication strategies during `StorageService` initialization (single-threaded context) and stores them in an immutable map, eliminating the lock and the circular callback.

## Example
```java
// BEFORE
public synchronized AbstractReplicationStrategy getReplicationStrategy(String table) {
    AbstractReplicationStrategy strat = replicationStrategies.get(table);
    if (strat == null) {
        strat = StorageService.getReplicationStrategy(tokenMetadata_, table);
        // constructor registers w/ TokenMetadata, may callback into SS → deadlock
        replicationStrategies.put(table, strat);
    }
    return strat;
}
```
```java
// AFTER (in constructor, before gossip starts):
replicationStrategies = new HashMap<>();
for (String table : DatabaseDescriptor.getNonSystemTables()) {
    replicationStrategies.put(table, getReplicationStrategy(tokenMetadata_, table));
}
replicationStrategies = Collections.unmodifiableMap(replicationStrategies);
// getReplicationStrategy() no longer synchronized, just a map lookup
```
